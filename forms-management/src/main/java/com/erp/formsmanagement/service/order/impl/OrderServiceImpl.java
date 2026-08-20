package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.OrderParty;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.api.ordermanagement.model.PartyOrdersResponse;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemRepository;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import com.erp.formsmanagement.mapper.order.OrderMapper;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderServiceImpl
    extends AbstractSpecificationServiceV2<OrderEntity, NewOrder, Order, Long>
    implements OrderService {

  private final OrderRepository orderRepository;
  private final PartyRepository partyRepository;
  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final OrderItemRepository orderItemRepository;

  public OrderServiceImpl(
      OrderRepository orderRepository,
      OrderMapper orderMapper,
      PartyRepository partyRepository,
      ClientOrderRequestRepository clientOrderRequestRepository,
      OrderItemRepository orderItemRepository) {
    super(orderRepository, orderMapper);
    this.orderRepository = orderRepository;
    this.partyRepository = partyRepository;
    this.clientOrderRequestRepository = clientOrderRequestRepository;
    this.orderItemRepository = orderItemRepository;
  }

  /**
   * Deletes an order and, if it originated from an approved client order request, that request too —
   * so an order the admin removes here also disappears from the client's "My Orders". The
   * {@code order_id} FK is {@code ON DELETE SET NULL}, so without this the request would linger
   * (still marked APPROVED, just unlinked). Look the request up before deleting the order, since the
   * delete would otherwise null out the link first.
   */
  @Override
  public void deleteById(Long partyId, Long id) {
    clientOrderRequestRepository.findAllByOrder_Id(id).forEach(clientOrderRequestRepository::delete);
    orderRepository.deleteById(id);
  }

  @Override
  @Transactional
  public void deleteItem(Long partyId, Long orderId, Long itemId) {
    OrderItemEntity item =
        orderItemRepository
            .findById(itemId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, itemId)));

    // Guard: the item must actually belong to the order in the path.
    if (item.getOrder() == null || !orderId.equals(item.getOrder().getId())) {
      throw new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, itemId));
    }

    // job_works + job_work_returns + order_dispatch all FK-cascade on order_item delete.
    orderItemRepository.deleteById(itemId);

    // The derived query below auto-flushes the pending delete first, so an order left with no
    // items is removed too (along with any linked client order request, via deleteById).
    if (orderItemRepository.findAllByOrderId(orderId).isEmpty()) {
      deleteById(partyId, orderId);
    }
  }

  @Override
  @Transactional
  public Order updateScrap(Long orderId, Double scrap) {
    OrderEntity order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, orderId)));
    order.setScrap(scrap);
    return mapper().toDomain(order);
  }

  // ── Merging orders ────────────────────────────────────────────────────────

  /**
   * Lines are combined when the plater would treat them as the same goods: same size, same finish.
   * Nothing looser — a different finish is a different batch, and merging it would put the wrong
   * thing on the chitthi.
   */
  private static String lineKey(OrderItemEntity item) {
    Long sizeId = item.getItemSize() == null ? null : item.getItemSize().getId();
    String plating = item.getPlating() == null ? "" : item.getPlating().trim().toLowerCase();
    return sizeId + "|" + plating;
  }

  /** True while nothing has left the building: no line at a plater, nothing dispatched. */
  private boolean isCreated(OrderEntity order) {
    if (order.getOrderItems() == null) {
      return true;
    }
    return order.getOrderItems().stream()
        .noneMatch(
            item ->
                (item.getJobWorks() != null && !item.getJobWorks().isEmpty())
                    || (item.getJobWorkAllocations() != null
                        && !item.getJobWorkAllocations().isEmpty())
                    || (item.getTotalDispatchedPc() != null && item.getTotalDispatchedPc() > 0));
  }

  private static double nullToZero(Double value) {
    return value == null ? 0d : value;
  }

  private static Double round3(Double value) {
    return value == null ? null : Math.round(value * 1000d) / 1000d;
  }

  /** Sums one nullable field across the group, null only when no line in it carried a value. */
  private static Double sumField(
      List<OrderItemEntity> items, Function<OrderItemEntity, Double> field) {
    if (items.stream().map(field).noneMatch(Objects::nonNull)) {
      return null;
    }
    return round3(items.stream().map(field).mapToDouble(OrderServiceImpl::nullToZero).sum());
  }

  @Override
  @Transactional
  public Order mergeOrders(List<Long> orderIds, Double scrap) {
    List<Long> ids =
        orderIds == null
            ? List.of()
            : orderIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.size() < 2) {
      throw new IllegalArgumentException("Merging needs at least two different orders");
    }

    List<OrderEntity> sources = new ArrayList<>();
    for (Long id : ids) {
      sources.add(
          orderRepository
              .findById(id)
              .orElseThrow(
                  () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id))));
    }

    // One party. Merging across parties would put two customers' goods on one chitthi.
    Set<Long> parties =
        sources.stream()
            .map(order -> order.getParty() == null ? null : order.getParty().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (parties.size() != 1 || parties.contains(null)) {
      throw new IllegalArgumentException("Only orders of the same party can be merged");
    }

    for (OrderEntity source : sources) {
      if (source.getMergedInto() != null) {
        throw new IllegalArgumentException(
            "Order " + source.getId() + " is already part of a merge");
      }
      if (!source.getMergedSources().isEmpty()) {
        throw new IllegalArgumentException(
            "Order " + source.getId() + " is itself a merged order; un-merge it first");
      }
      if (!isCreated(source)) {
        throw new IllegalArgumentException(
            "Order " + source.getId() + " has already gone to job work or been dispatched");
      }
    }

    // Oldest first, so the merged order inherits the earliest P/O date and the sources read in the
    // order the party placed them.
    sources.sort(
        Comparator.comparing(
                OrderEntity::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OrderEntity::getId));

    OrderEntity merged = new OrderEntity();
    merged.setParty(sources.get(0).getParty());
    merged.setOrderDate(sources.get(0).getOrderDate());
    merged.setScrap(scrap);
    merged.setOrderItems(new ArrayList<>());
    orderRepository.save(merged);

    // Group every line of every source by what the plater sees. A group of one is a line that had
    // no counterpart and simply rides across; a group of several is a genuine sum.
    Map<String, List<OrderItemEntity>> groups = new LinkedHashMap<>();
    for (OrderEntity source : sources) {
      for (OrderItemEntity item : source.getOrderItems()) {
        groups.computeIfAbsent(lineKey(item), key -> new ArrayList<>()).add(item);
      }
    }

    for (List<OrderItemEntity> group : groups.values()) {
      OrderItemEntity first = group.get(0);
      OrderItemEntity line = new OrderItemEntity();
      line.setOrder(merged);
      line.setItemSize(first.getItemSize());
      line.setPlating(first.getPlating());

      line.setQtyPc(sumField(group, OrderItemEntity::getQtyPc));
      line.setQtyKg(sumField(group, OrderItemEntity::getQtyKg));
      line.setPendingPc(sumField(group, OrderItemEntity::getPendingPc));
      line.setStickerQty(sumField(group, OrderItemEntity::getStickerQty));

      // Packing figures are rates for this size and client, not quantities — the same on every
      // line of the group, so they are carried over rather than added up.
      line.setPcPerBox(first.getPcPerBox());
      line.setBoxPerCartoon(first.getBoxPerCartoon());
      line.setPcPerCartoon(first.getPcPerCartoon());
      line.setPlatingType(first.getPlatingType());
      line.setJobActionDone(Boolean.FALSE);

      orderItemRepository.save(line);
      merged.getOrderItems().add(line);

      // The source lines keep their own quantities untouched; they just point at the line that now
      // carries the combined one. Both sides are set: the response to this call is built from the
      // same in-session graph, and an inverse side left empty would report a merge covering
      // nothing.
      group.forEach(
          item -> {
            item.setMergedIntoItem(line);
            line.getMergedSourceItems().add(item);
          });
    }

    sources.forEach(
        source -> {
          source.setMergedInto(merged);
          merged.getMergedSources().add(source);
        });
    orderRepository.flush();

    return mapper().toDomain(merged);
  }

  @Override
  @Transactional
  public void unmergeOrder(Long orderId) {
    OrderEntity merged =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, orderId)));

    if (merged.getMergedSources().isEmpty()) {
      throw new IllegalArgumentException("Order " + orderId + " is not a merged order");
    }
    if (!isCreated(merged)) {
      throw new IllegalArgumentException(
          "Order " + orderId + " has already gone to job work or been dispatched");
    }

    // Nothing has to be rebuilt: the sources were never altered. Releasing them and dropping the
    // merged order is the whole of it.
    merged.getMergedSources().forEach(source -> source.setMergedInto(null));
    merged
        .getOrderItems()
        .forEach(line -> line.getMergedSourceItems().forEach(item -> item.setMergedIntoItem(null)));
    merged.getMergedSources().clear();

    // deleteById would also delete the client requests behind the sources; the merged order has
    // none of its own, so it is removed directly.
    orderRepository.delete(merged);
    orderRepository.flush();
  }

  @Override
  protected void afterCreate(OrderEntity entity, Long partyId, NewOrder request) {
    entity.setParty(
        partyRepository
            .findById(partyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, partyId))));
  }

  @Override
  protected void afterUpdate(OrderEntity entity, Long partyId, NewOrder request) {
    afterCreate(entity, partyId, request);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultOrder getAll(Long partyId, GetAllQuery<String> query) {
    var party =
        partyRepository
            .findById(partyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, partyId)));
    Specification<OrderEntity> spec =
        Specification.<OrderEntity>where((root, q, cb) -> cb.equal(root.get("party").get("id"), partyId))
            .and(orderRepository.notMergedAway());
    Page<OrderEntity> page =
        orderRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        page,
        mapper()::toDomain,
        PaginatedResultOrder::new,
        (response, orders) -> {
          PartyOrdersResponse wrapper = new PartyOrdersResponse();
          wrapper.setParty(new OrderParty().id(party.getId()).name(party.getName()));
          wrapper.setOrders(orders);
          response.setData(wrapper);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedPartyOrdersResponse getAll(GetAllQuery<String> query) {
    Page<PartyEntity> partyPage =
        partyRepository.findAll(
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    List<Long> partyIds = partyPage.getContent().stream().map(PartyEntity::getId).toList();

    Map<Long, List<Order>> ordersByParty =
        orderRepository.findByParty_IdInAndMergedIntoIsNull(partyIds).stream()
            .collect(
                Collectors.groupingBy(
                    oe -> oe.getParty().getId(),
                    Collectors.mapping(mapper()::toDomain, Collectors.toList())));

    return PageMapper.toResult(
        partyPage,
        p -> p,
        PaginatedPartyOrdersResponse::new,
        (response, ignored) -> {
          List<PartyOrdersResponse> data = new ArrayList<>();
          for (PartyEntity party : partyPage.getContent()) {
            PartyOrdersResponse por = new PartyOrdersResponse();
            por.setParty(new OrderParty().id(party.getId()).name(party.getName()));
            por.setOrders(ordersByParty.getOrDefault(party.getId(), new ArrayList<>()));
            data.add(por);
          }
          response.setData(data);
        });
  }
}

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
import java.util.List;
import java.util.Map;
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
        (root, q, cb) -> cb.equal(root.get("party").get("id"), partyId);
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
        orderRepository.findByParty_IdIn(partyIds).stream()
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

package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.NewOrderItem;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.OrderParty;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.api.ordermanagement.model.PartyOrdersResponse;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkOrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.MergeMoveKind;
import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemMergeMoveEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemMergeMoveRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemRepository;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import com.erp.formsmanagement.mapper.order.OrderItemMapper;
import com.erp.formsmanagement.mapper.order.OrderMapper;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
  private final OrderItemMapper orderItemMapper;
  private final OrderItemMergeMoveRepository mergeMoveRepository;
  private final ClientOrderFulfillmentService clientOrderFulfillmentService;

  public OrderServiceImpl(
      OrderRepository orderRepository,
      OrderMapper orderMapper,
      PartyRepository partyRepository,
      ClientOrderRequestRepository clientOrderRequestRepository,
      OrderItemRepository orderItemRepository,
      OrderItemMapper orderItemMapper,
      OrderItemMergeMoveRepository mergeMoveRepository,
      ClientOrderFulfillmentService clientOrderFulfillmentService) {
    super(orderRepository, orderMapper);
    this.orderRepository = orderRepository;
    this.partyRepository = partyRepository;
    this.clientOrderRequestRepository = clientOrderRequestRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderItemMapper = orderItemMapper;
    this.mergeMoveRepository = mergeMoveRepository;
    this.clientOrderFulfillmentService = clientOrderFulfillmentService;
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
    orderRepository.findById(id).ifPresent(OrderServiceImpl::refuseDeletingMerged);
    clientOrderRequestRepository.findAllByOrder_Id(id).forEach(clientOrderRequestRepository::delete);
    orderRepository.deleteById(id);
  }

  /**
   * A merged line carries the dispatches and chitthis moved off the lines it was made from, and a
   * line row takes its dispatches and chitthis with it when deleted — so deleting a merged order
   * would erase real history. Equally, an order with lines folded into a merge is still needed by
   * that merge. Both have to be un-merged first.
   */
  private static void refuseDeletingMerged(OrderEntity order) {
    List<OrderItemEntity> lines = order.getOrderItems() == null ? List.of() : order.getOrderItems();
    if (!order.getMergedSources().isEmpty() || lines.stream().anyMatch(OrderItemEntity::isMergedLine)) {
      throw new IllegalArgumentException("This is a merged order — un-merge it before deleting");
    }
    if (order.getMergedInto() != null || lines.stream().anyMatch(OrderItemEntity::isMergedAway)) {
      throw new IllegalArgumentException(
          "Lines of this order are merged into another order — un-merge that order before deleting this one");
    }
  }

  /** The lines the order sheet shows: everything except lines folded into a merged line. */
  private static List<OrderItemEntity> visibleLines(OrderEntity order) {
    List<OrderItemEntity> lines = order.getOrderItems();
    return lines == null ? List.of() : lines.stream().filter(line -> !line.isMergedAway()).toList();
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
    // Same reason as refuseDeletingMerged: the row would take the moved history down with it.
    if (item.isMergedLine()) {
      throw new IllegalArgumentException("This is a merged line — un-merge it before deleting");
    }
    if (item.isMergedAway()) {
      throw new IllegalArgumentException(
          "This line is merged into another order — un-merge that order before deleting it");
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

  @Override
  @Transactional
  public Order updateCompleted(Long orderId, boolean completed) {
    OrderEntity order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, orderId)));
    order.setCompleted(completed);
    // The order's tick means "every line of it", so it carries down. Otherwise an order ticked
    // whole would still show its lines as outstanding on a sheet that now reads the line. Lines
    // folded into a merge are left alone: they are finished through the merged line.
    visibleLines(order).forEach(line -> line.setCompleted(completed));
    return mapper().toDomain(order);
  }

  /**
   * Ticks one line. Keyed on the line's own id, like the job-work status endpoint above it: the
   * caller has the row in front of it and no business resending the order to change one boolean.
   *
   * <p>The parent order follows the lines rather than leading them -- it reads as completed once
   * every line is, and reopens the moment one is un-ticked -- so the two can never disagree.
   */
  @Override
  @Transactional
  public Order updateItemCompleted(Long orderItemId, boolean completed) {
    OrderItemEntity item =
        orderItemRepository
            .findById(orderItemId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, orderItemId)));
    item.setCompleted(completed);

    OrderEntity order = item.getOrder();
    if (order == null) {
      throw new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, orderItemId));
    }
    // A line folded into a merge is never ticked itself, so it must not hold the order open.
    List<OrderItemEntity> lines = visibleLines(order);
    order.setCompleted(!lines.isEmpty() && lines.stream().allMatch(OrderItemEntity::isCompleted));
    return mapper().toDomain(order);
  }

  // ── Merging order lines ───────────────────────────────────────────────────

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

  private static double nullToZero(Double value) {
    return value == null ? 0d : value;
  }

  private static Double round3(Double value) {
    return value == null ? null : Math.round(value * 1000d) / 1000d;
  }

  private static boolean sameQty(Double a, Double b) {
    return Math.abs(nullToZero(a) - nullToZero(b)) < 0.001;
  }

  /** Adds two nullable figures, null only when neither carried a value. */
  private static Double addNullable(Double a, Double b) {
    if (a == null && b == null) {
      return null;
    }
    return round3(nullToZero(a) + nullToZero(b));
  }

  private static Double pcsWeight(OrderItemEntity item) {
    Double weight = item.getItemSize() == null ? null : item.getItemSize().getPcsWeight();
    return weight != null && weight > 0 ? weight : null;
  }

  /** What the line is packed by: its own figure, else the size's item master — as the sheet shows. */
  private static Double packing(Double own, Integer master) {
    if (own != null) {
      return own;
    }
    return master == null ? null : master.doubleValue();
  }

  /** The three packing figures a carton count is worked out from, for comparing two lines. */
  private static List<Double> packingOf(OrderItemEntity item) {
    InventoryEntity inventory = item.getItemSize() == null ? null : item.getItemSize().getInventory();
    return Arrays.asList(
        packing(item.getPcPerBox(), inventory == null ? null : inventory.getPcsPerBox()),
        packing(item.getBoxPerCartoon(), inventory == null ? null : inventory.getBoxPerCarton()),
        packing(item.getPcPerCartoon(), inventory == null ? null : inventory.getPcsPerCarton()));
  }

  private static boolean samePacking(List<Double> a, List<Double> b) {
    for (int i = 0; i < a.size(); i++) {
      Double x = a.get(i);
      Double y = b.get(i);
      if (x == null && y == null) continue;
      if (x == null || y == null || Math.abs(x - y) > 0.0005) return false;
    }
    return true;
  }

  /** How the sheet names a line in a message: "Z-3 (2.5mm) 3 x 1/2 S.S.". */
  private static String describeLine(OrderItemEntity item) {
    StringBuilder name = new StringBuilder();
    if (item.getItemSize() != null) {
      if (item.getItemSize().getItem() != null) {
        name.append(item.getItemSize().getItem().getItemName()).append(' ');
      }
      if (item.getItemSize().getSizeInInch() != null) {
        name.append(item.getItemSize().getSizeInInch()).append(' ');
      }
    }
    if (item.getPlating() != null) {
      name.append(item.getPlating());
    }
    return name.toString().trim();
  }

  /**
   * One line's quantity as pieces and Kg. A line ordered in only one unit is converted through the
   * size's 1-pc weight, so a Kg-only line and a pc-only line still add up correctly.
   */
  private record Qty(Double pc, Double kg) {

    static Qty of(Double pc, Double kg, Double pcsWeight) {
      Double pcs = pc;
      Double kgs = kg;
      if ((pcs == null || pcs <= 0) && kgs != null && kgs > 0 && pcsWeight != null) {
        pcs = kgs / pcsWeight;
      }
      if ((kgs == null || kgs <= 0) && pcs != null && pcs > 0 && pcsWeight != null) {
        kgs = pcs * pcsWeight;
      }
      return new Qty(pcs, kgs);
    }
  }

  /**
   * Folds order lines together. See {@link OrderService#mergeOrders}.
   *
   * <p>Every check runs before anything is changed, so a refused merge leaves the book exactly as
   * it was.
   */
  @Override
  @Transactional
  public Order mergeOrders(List<Long> orderItemIds, Double scrap) {
    List<Long> ids =
        orderItemIds == null
            ? List.of()
            : orderItemIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.size() < 2) {
      throw new IllegalArgumentException("Pick at least two lines to merge");
    }

    List<OrderItemEntity> lines = new ArrayList<>();
    for (Long id : ids) {
      lines.add(
          orderItemRepository
              .findById(id)
              .orElseThrow(
                  () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id))));
    }

    // One party. Merging across parties would put two customers' goods on one chitthi.
    Set<Long> parties =
        lines.stream()
            .map(
                line ->
                    line.getOrder() == null || line.getOrder().getParty() == null
                        ? null
                        : line.getOrder().getParty().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (parties.size() != 1 || parties.contains(null)) {
      throw new IllegalArgumentException("Only lines of the same party can be merged");
    }

    for (OrderItemEntity line : lines) {
      if (line.isMergedAway()) {
        throw new IllegalArgumentException(describeLine(line) + " is already part of a merge");
      }
      // The tick means the works is finished with the line; adding it to another would reopen it.
      if (line.isCompleted()) {
        throw new IllegalArgumentException(
            describeLine(line) + " is completed — completed lines can't be merged");
      }
    }

    Map<String, List<OrderItemEntity>> groups = new LinkedHashMap<>();
    lines.forEach(line -> groups.computeIfAbsent(lineKey(line), key -> new ArrayList<>()).add(line));

    for (List<OrderItemEntity> group : groups.values()) {
      OrderItemEntity first = group.get(0);
      if (group.size() < 2) {
        throw new IllegalArgumentException(
            describeLine(first) + " has no other selected line with the same size and plating");
      }
      // Carton and sticker counts on the chitthi come from the packing, so lines packed
      // differently are different goods as far as the floor is concerned.
      List<Double> packing = packingOf(first);
      for (OrderItemEntity line : group) {
        if (!samePacking(packing, packingOf(line))) {
          throw new IllegalArgumentException(
              "Packing differs for "
                  + describeLine(first)
                  + " — Pc/Box, Box/Carton and Pc/Carton must match to merge");
        }
      }
      if (group.stream().filter(OrderItemEntity::isMergedLine).count() > 1) {
        throw new IllegalArgumentException(
            "Two merged lines of " + describeLine(first) + " can't be merged together — un-merge one first");
      }
    }

    // An already-merged line in the selection is joined rather than merged again, so every group
    // has to land on that line's order. Two different merged orders cannot both be the target.
    Set<OrderEntity> mergedOrders =
        lines.stream()
            .filter(OrderItemEntity::isMergedLine)
            .map(OrderItemEntity::getOrder)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (mergedOrders.size() > 1) {
      throw new IllegalArgumentException(
          "These lines belong to two different merged orders — un-merge one first");
    }

    List<OrderItemEntity> sources =
        lines.stream()
            .filter(line -> !line.isMergedLine())
            .sorted(
                Comparator.comparing(
                        (OrderItemEntity line) -> line.getOrder().getOrderDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(OrderItemEntity::getId))
            .toList();
    LocalDate earliest =
        sources.stream()
            .map(line -> line.getOrder().getOrderDate())
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);

    OrderEntity merged;
    if (mergedOrders.isEmpty()) {
      merged = new OrderEntity();
      merged.setParty(sources.get(0).getOrder().getParty());
      merged.setOrderDate(earliest);
      merged.setScrap(scrap);
      merged.setOrderItems(new ArrayList<>());
      orderRepository.save(merged);
    } else {
      merged = mergedOrders.iterator().next();
      if (earliest != null
          && (merged.getOrderDate() == null || earliest.isBefore(merged.getOrderDate()))) {
        merged.setOrderDate(earliest);
      }
      if (scrap != null) {
        merged.setScrap(scrap);
      }
    }

    Set<Long> touchedOrderIds = new LinkedHashSet<>();
    for (List<OrderItemEntity> group : groups.values()) {
      OrderItemEntity target =
          group.stream().filter(OrderItemEntity::isMergedLine).findFirst().orElse(null);
      List<OrderItemEntity> joining =
          sources.stream().filter(group::contains).toList();
      List<Qty> contributions = new ArrayList<>();

      if (target == null) {
        OrderItemEntity first = joining.get(0);
        target = new OrderItemEntity();
        target.setOrder(merged);
        target.setItemSize(first.getItemSize());
        target.setPlating(first.getPlating());
        // Packing is a rate, identical across the group (checked above), so it is carried over.
        target.setPcPerBox(first.getPcPerBox());
        target.setBoxPerCartoon(first.getBoxPerCartoon());
        target.setPcPerCartoon(first.getPcPerCartoon());
        target.setPlatingType(first.getPlatingType());
        target.setJobActionDone(Boolean.FALSE);
        orderItemRepository.save(target);
        merged.getOrderItems().add(target);
      } else {
        contributions.add(Qty.of(target.getQtyPc(), target.getQtyKg(), pcsWeight(target)));
      }

      Double stickerQty = target.getStickerQty();
      boolean anyPc = target.getQtyPc() != null;
      boolean anyKg = target.getQtyKg() != null;
      for (OrderItemEntity source : joining) {
        contributions.add(Qty.of(source.getQtyPc(), source.getQtyKg(), pcsWeight(source)));
        anyPc |= source.getQtyPc() != null;
        anyKg |= source.getQtyKg() != null;
        stickerQty = addNullable(stickerQty, source.getStickerQty());
        absorb(target, source);
        touchedOrderIds.add(source.getOrder().getId());
      }

      // A unit is filled in only if some line was ordered in it, so a sheet of pc-only orders
      // keeps reading as pc-only. Where it is filled in, every line counts, converted if need be.
      target.setQtyPc(anyPc ? round3(contributions.stream().mapToDouble(q -> nullToZero(q.pc())).sum()) : null);
      target.setQtyKg(anyKg ? round3(contributions.stream().mapToDouble(q -> nullToZero(q.kg())).sum()) : null);
      target.setStickerQty(stickerQty);

      double dispatched =
          target.getDispatches().stream().mapToDouble(d -> nullToZero(d.getDispatchPcs())).sum();
      target.setPendingPc(
          target.getQtyPc() == null ? null : round3(Math.max(0d, target.getQtyPc() - dispatched)));
    }

    orderRepository.flush();
    touchedOrderIds.add(merged.getId());
    touchedOrderIds.forEach(clientOrderFulfillmentService::syncByOrderId);
    return mapper().toDomain(merged);
  }

  /**
   * Moves one source line onto the merged line: its dispatches, the chitthis it is the primary line
   * of, and its share of chitthis covering several lines. Each move is recorded, so un-merge can
   * put it back and the client portal can keep crediting it to the order it was done for.
   *
   * <p>The source's own quantities are never touched — that is what the party ordered.
   */
  private void absorb(OrderItemEntity target, OrderItemEntity source) {
    for (OrderDispatchEntity dispatch : List.copyOf(source.getDispatches())) {
      recordMove(target, source, MergeMoveKind.DISPATCH, dispatch.getId(), null, dispatch.getDispatchPcs());
      dispatch.setOrderItem(target);
      source.getDispatches().remove(dispatch);
      target.getDispatches().add(dispatch);
    }

    for (JobWorkEntity jobWork : List.copyOf(source.getJobWorks())) {
      recordMove(target, source, MergeMoveKind.JOB_WORK, jobWork.getId(), jobWork.getQtyKg(), jobWork.getQtyPc());
      jobWork.setOrderItem(target);
      source.getJobWorks().remove(jobWork);
      target.getJobWorks().add(jobWork);
    }

    for (JobWorkOrderItemEntity share : List.copyOf(source.getJobWorkAllocations())) {
      JobWorkEntity jobWork = share.getJobWork();
      // A chitthi may hold only one share per line, so when it already has one for the merged line
      // (it covered two of the lines being merged) the two become one. The move rows keep each
      // original share, which is how un-merge splits it back.
      JobWorkOrderItemEntity onTarget =
          jobWork.getMergedOrderItems().stream()
              .filter(other -> other != share && other.getOrderItem() != null && target.getId().equals(other.getOrderItem().getId()))
              .findFirst()
              .orElse(null);
      if (onTarget != null) {
        recordMove(target, source, MergeMoveKind.ALLOCATION, onTarget.getId(), share.getQtyKg(), share.getQtyPc());
        onTarget.setQtyKg(addNullable(onTarget.getQtyKg(), share.getQtyKg()));
        onTarget.setQtyPc(addNullable(onTarget.getQtyPc(), share.getQtyPc()));
        jobWork.getMergedOrderItems().remove(share);
      } else {
        recordMove(target, source, MergeMoveKind.ALLOCATION, share.getId(), share.getQtyKg(), share.getQtyPc());
        share.setOrderItem(target);
        target.getJobWorkAllocations().add(share);
      }
      source.getJobWorkAllocations().remove(share);
    }

    target.setJobActionDone(
        Boolean.TRUE.equals(target.getJobActionDone()) || Boolean.TRUE.equals(source.getJobActionDone()));
    if (target.getPlatingType() == null) {
      target.setPlatingType(source.getPlatingType());
    }
    if (target.getJobWorkNo() == null) {
      target.setJobWorkNo(source.getJobWorkNo());
    }

    source.setMergedIntoItem(target);
    target.getMergedSourceItems().add(source);
  }

  private void recordMove(
      OrderItemEntity target,
      OrderItemEntity source,
      MergeMoveKind kind,
      Long recordId,
      Double qtyKg,
      Double qtyPc) {
    OrderItemMergeMoveEntity move = new OrderItemMergeMoveEntity();
    move.setMergedItem(target);
    move.setSourceItem(source);
    move.setKind(kind);
    move.setRecordId(recordId);
    move.setQtyKg(qtyKg);
    move.setQtyPc(qtyPc);
    mergeMoveRepository.save(move);
    target.getMergeMoves().add(move);
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

    List<OrderItemEntity> mergedLines =
        merged.getOrderItems() == null
            ? List.of()
            : merged.getOrderItems().stream().filter(OrderItemEntity::isMergedLine).toList();
    if (mergedLines.isEmpty() && merged.getMergedSources().isEmpty()) {
      throw new IllegalArgumentException("Order " + orderId + " is not a merged order");
    }

    // Every line is checked before any is touched, so a refusal changes nothing.
    for (OrderItemEntity line : mergedLines) {
      if (changedSinceMerge(line)) {
        throw new IllegalArgumentException(
            "Can't un-merge: a dispatch or chitthi on "
                + describeLine(line)
                + " was recorded or changed after merging");
      }
    }

    Set<Long> touchedOrderIds = new LinkedHashSet<>();
    for (OrderItemEntity line : mergedLines) {
      line.getMergedSourceItems().forEach(source -> touchedOrderIds.add(source.getOrder().getId()));
      restore(line);
      merged.getOrderItems().remove(line);
    }

    // Orders merged before lines could be merged on their own were linked at the order level too.
    merged.getMergedSources().forEach(source -> source.setMergedInto(null));
    merged.getMergedSources().clear();

    // A line added to the merged order by hand after the merge is not part of the merge and stays
    // where it is; the order only goes once nothing is left on it. deleteById would also delete the
    // client requests behind the sources; the merged order has none of its own.
    if (merged.getOrderItems().isEmpty()) {
      orderRepository.delete(merged);
    }
    orderRepository.flush();
    touchedOrderIds.forEach(clientOrderFulfillmentService::syncByOrderId);
  }

  /**
   * True when the merged line carries anything that is not exactly what moved onto it: a dispatch
   * or chitthi recorded since, or a moved one edited or deleted. Such work belongs to the combined
   * quantity and there is no honest way to divide it back.
   */
  private boolean changedSinceMerge(OrderItemEntity line) {
    List<OrderItemMergeMoveEntity> moves = line.getMergeMoves();

    Map<Long, OrderItemMergeMoveEntity> movedDispatches = new HashMap<>();
    Map<Long, OrderItemMergeMoveEntity> movedJobWorks = new HashMap<>();
    Map<Long, List<OrderItemMergeMoveEntity>> movedShares = new HashMap<>();
    for (OrderItemMergeMoveEntity move : moves) {
      switch (move.getKind()) {
        case DISPATCH -> movedDispatches.put(move.getRecordId(), move);
        case JOB_WORK -> movedJobWorks.put(move.getRecordId(), move);
        case ALLOCATION ->
            movedShares.computeIfAbsent(move.getRecordId(), id -> new ArrayList<>()).add(move);
      }
    }

    if (line.getDispatches().size() != movedDispatches.size()) {
      return true;
    }
    for (OrderDispatchEntity dispatch : line.getDispatches()) {
      OrderItemMergeMoveEntity move = movedDispatches.get(dispatch.getId());
      if (move == null || !sameQty(move.getQtyPc(), dispatch.getDispatchPcs())) {
        return true;
      }
    }

    if (line.getJobWorks().size() != movedJobWorks.size()) {
      return true;
    }
    for (JobWorkEntity jobWork : line.getJobWorks()) {
      OrderItemMergeMoveEntity move = movedJobWorks.get(jobWork.getId());
      if (move == null || !sameQty(move.getQtyKg(), jobWork.getQtyKg())) {
        return true;
      }
    }

    if (line.getJobWorkAllocations().size() != movedShares.size()) {
      return true;
    }
    for (JobWorkOrderItemEntity share : line.getJobWorkAllocations()) {
      List<OrderItemMergeMoveEntity> parts = movedShares.get(share.getId());
      if (parts == null) {
        return true;
      }
      double movedKg = parts.stream().mapToDouble(m -> nullToZero(m.getQtyKg())).sum();
      if (!sameQty(movedKg, share.getQtyKg())) {
        return true;
      }
    }
    return false;
  }

  /** Puts every moved record back on the line it came from and releases the sources. */
  private void restore(OrderItemEntity line) {
    Map<Long, OrderDispatchEntity> dispatches = new HashMap<>();
    line.getDispatches().forEach(d -> dispatches.put(d.getId(), d));
    Map<Long, JobWorkEntity> jobWorks = new HashMap<>();
    line.getJobWorks().forEach(jw -> jobWorks.put(jw.getId(), jw));
    Map<Long, JobWorkOrderItemEntity> shares = new HashMap<>();
    line.getJobWorkAllocations().forEach(a -> shares.put(a.getId(), a));

    Map<Long, List<OrderItemMergeMoveEntity>> sharesById = new LinkedHashMap<>();
    for (OrderItemMergeMoveEntity move : line.getMergeMoves()) {
      OrderItemEntity source = move.getSourceItem();
      switch (move.getKind()) {
        case DISPATCH -> {
          OrderDispatchEntity dispatch = dispatches.get(move.getRecordId());
          dispatch.setOrderItem(source);
          line.getDispatches().remove(dispatch);
          source.getDispatches().add(dispatch);
        }
        case JOB_WORK -> {
          JobWorkEntity jobWork = jobWorks.get(move.getRecordId());
          jobWork.setOrderItem(source);
          line.getJobWorks().remove(jobWork);
          source.getJobWorks().add(jobWork);
        }
        case ALLOCATION ->
            sharesById.computeIfAbsent(move.getRecordId(), id -> new ArrayList<>()).add(move);
      }
    }

    // A share that was two shares before the merge is split back: the row goes to the first line
    // with its own figures, and the others get rows of their own again.
    sharesById.forEach(
        (shareId, parts) -> {
          JobWorkOrderItemEntity share = shares.get(shareId);
          line.getJobWorkAllocations().remove(share);
          for (int i = 0; i < parts.size(); i++) {
            OrderItemMergeMoveEntity part = parts.get(i);
            JobWorkOrderItemEntity row = share;
            if (i > 0) {
              row = new JobWorkOrderItemEntity();
              row.setJobWork(share.getJobWork());
              share.getJobWork().getMergedOrderItems().add(row);
            }
            row.setOrderItem(part.getSourceItem());
            row.setQtyKg(part.getQtyKg());
            row.setQtyPc(part.getQtyPc());
            part.getSourceItem().getJobWorkAllocations().add(row);
          }
        });

    mergeMoveRepository.deleteAll(List.copyOf(line.getMergeMoves()));
    line.getMergeMoves().clear();
    line.getMergedSourceItems().forEach(source -> source.setMergedIntoItem(null));
    line.getMergedSourceItems().clear();
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
    reconcileItems(entity, request);
  }

  /**
   * Applies an edit to an order's lines <em>by id</em>, in place, instead of rebuilding the whole
   * collection.
   *
   * <p>The order sheet edits one line but resends every line on the order, so the naive mapping
   * ({@code orderItems = items}) cleared the collection and inserted fresh rows for all of them.
   * With {@code orphanRemoval=true} and a delete-cascade on {@code order_item}, that silently took
   * each line's dispatches, job works and returns down with the old rows, and every new row came
   * back with {@code completed=false} — so an order dispatched and ticked complete fell back to
   * Active with its history gone the moment anything on it was edited.
   *
   * <p>Now a line carrying its {@code id} is matched to the existing row and only its editable
   * fields are copied over; its children and its completed tick are never touched. A line with no
   * id is a genuine addition. A line that exists on the order but is absent from the edit is a
   * genuine removal — it is dropped, and orphan-removal + the FK cascade take its history with it
   * (removal is intended here, unlike the wholesale wipe this replaced).
   */
  private void reconcileItems(OrderEntity order, NewOrder request) {
    List<NewOrderItem> requested = request.getItems() == null ? List.of() : request.getItems();
    if (order.getOrderItems() == null) {
      order.setOrderItems(new ArrayList<>());
    }
    List<OrderItemEntity> existing = order.getOrderItems();

    Map<Long, OrderItemEntity> byId =
        existing.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(OrderItemEntity::getId, Function.identity(), (a, b) -> a));

    Set<Long> keptIds = new LinkedHashSet<>();
    List<OrderItemEntity> toAdd = new ArrayList<>();

    for (NewOrderItem req : requested) {
      OrderItemEntity target = req.getId() == null ? null : byId.get(req.getId());
      if (target != null && target.isMergedAway()) {
        // Hidden from the sheet, so nothing legitimate edits it; leave it as the merge needs it.
        keptIds.add(target.getId());
        continue;
      }
      if (target != null) {
        // Editable fields only — updateEntity ignores id / order / itemSize / completed and the
        // merge links, so dispatches, job works, returns and the completed tick all survive.
        orderItemMapper.updateEntity(target, req);
        target.setOrder(order);
        keptIds.add(target.getId());
      } else {
        OrderItemEntity created = orderItemMapper.toEntity(req);
        created.setOrder(order);
        toAdd.add(created);
      }
    }

    // Mutate the managed collection in place — never swap the reference, or orphan-removal trips.
    // Lines folded into a merge are not on the sheet, so the edit never resends them; their absence
    // is not a removal, and dropping them would orphan the merged line built from them.
    existing.removeIf(
        item ->
            item.getId() != null && !keptIds.contains(item.getId()) && !item.isMergedAway());
    existing.addAll(toAdd);
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
        orderRepository
            .findAll(
                Specification.<OrderEntity>where(
                        (root, q, cb) -> root.get("party").get("id").in(partyIds))
                    .and(orderRepository.notMergedAway()))
            .stream()
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

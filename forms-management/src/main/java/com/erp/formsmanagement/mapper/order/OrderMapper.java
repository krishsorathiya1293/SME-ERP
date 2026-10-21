package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.MergedOrderSource;
import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.OrderStatus;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.mapper.EntityMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(
    componentModel = "spring",
    uses = {OrderItemMapper.class})
public interface OrderMapper extends EntityMapper<OrderEntity, NewOrder, Order> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "completed", ignore = true)
  @Mapping(target = "mergedInto", ignore = true)
  @Mapping(target = "mergedSources", ignore = true)
  @Mapping(target = "orderItems", source = "items")
  OrderEntity toEntity(NewOrder newOrder);

  /**
   * The scrap is deliberately not updated here. Editing an order means resending every line, and
   * the sheet that does so has no reason to know the agreed scrap — leaving it mapped meant any
   * ordinary edit silently cleared it. It is set once on create and changed through its own
   * endpoint after that. The completed tick is left out for exactly the same reason.
   *
   * <p>{@code orderItems} is left out too, and reconciled by id in
   * {@link com.erp.formsmanagement.service.order.impl.OrderServiceImpl#afterUpdate} instead. If it
   * were mapped, MapStruct would clear the collection and rebuild it from scratch — and since the
   * collection is {@code orphanRemoval=true} with a delete-cascade behind it, that quietly deleted
   * every edited order's dispatches, job works and returns, and reset each line's completed tick.
   * Reconciling by id keeps the rows the edit did not touch exactly as they were.
   */
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "scrap", ignore = true)
  @Mapping(target = "completed", ignore = true)
  @Mapping(target = "mergedInto", ignore = true)
  @Mapping(target = "mergedSources", ignore = true)
  @Mapping(target = "orderItems", ignore = true)
  void updateEntity(@MappingTarget OrderEntity entity, NewOrder newOrder);

  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toLocalDate")
  @Mapping(target = "status", expression = "java(toStatus(entity))")
  @Mapping(target = "mergedFrom", expression = "java(toMergedFrom(entity))")
  Order toDomain(OrderEntity entity);

  List<Order> toDomainList(List<OrderEntity> entities);

  @Named("toLocalDate")
  default LocalDate toLocalDate(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.toLocalDate();
  }

  /**
   * How far the order has moved, worked out from the works rather than stored on the row.
   *
   * <p>Derived because a stored status would have to be rewritten on every job-work and dispatch
   * create <em>and</em> delete, and goes quietly wrong the first time one of those is missed —
   * which matters here, since this is what decides whether an order may be merged. The same
   * reasoning as the job-work return state, which is likewise computed rather than kept.
   *
   * <p>CREATED means nothing has left the building: no line sent to a plater, nothing dispatched.
   * DISPATCHED means every line has started going out. Anything in between is IN_JOB_WORK.
   *
   * <p>A line counts as dispatched on its FIRST piece, not its last. The works asked for this: a
   * line is finished in their eyes once the goods are on the lorry, and the tail — a short
   * delivery settled by agreement, a balance written off — is often never recorded as a movement,
   * so waiting for the full count left lines open forever. The quantities are untouched and still
   * say exactly how much went, so nothing is hidden by the flag moving earlier.
   */
  default OrderStatus toStatus(OrderEntity entity) {
    // Lines folded into a merge carry nothing any more — their dispatches and chitthis moved to the
    // merged line — so they would hold the order back from DISPATCHED forever. They are read
    // through the merged line instead.
    List<OrderItemEntity> items =
        entity.getOrderItems() == null
            ? List.of()
            : entity.getOrderItems().stream().filter(item -> !item.isMergedAway()).toList();
    if (items.isEmpty()) {
      return OrderStatus.CREATED;
    }

    boolean anyMovement = false;
    boolean allDispatched = true;
    for (OrderItemEntity item : items) {
      double dispatched = item.getTotalDispatchedPc() == null ? 0d : item.getTotalDispatchedPc();
      double ordered = item.getQtyPc() == null ? 0d : item.getQtyPc();
      boolean sent =
          (item.getJobWorks() != null && !item.getJobWorks().isEmpty())
              || (item.getJobWorkAllocations() != null && !item.getJobWorkAllocations().isEmpty());

      if (sent || dispatched > 0) {
        anyMovement = true;
      }
      if (dispatched <= 0) {
        allDispatched = false;
      }
    }

    if (allDispatched) {
      return OrderStatus.DISPATCHED;
    }
    return anyMovement ? OrderStatus.IN_JOB_WORK : OrderStatus.CREATED;
  }

  /**
   * The orders folded into this one. Each keeps its own P/O date — that is a fact about the
   * party's purchase order and cannot be averaged away — which is why a merged order shows several
   * dates rather than one.
   */
  default List<MergedOrderSource> toMergedFrom(OrderEntity entity) {
    // Read off the lines: a line merge folds in lines, not whole orders, so one source order may
    // give a line to this merge and keep the rest. Orders merged before that were linked at the
    // order level as well, and their lines point here too, so they come out the same.
    Map<Long, OrderEntity> sources = new LinkedHashMap<>();
    if (entity.getOrderItems() != null) {
      entity.getOrderItems().stream()
          .flatMap(line -> line.getMergedSourceItems().stream())
          .map(OrderItemEntity::getOrder)
          .filter(Objects::nonNull)
          .forEach(order -> sources.putIfAbsent(order.getId(), order));
    }
    if (entity.getMergedSources() != null) {
      entity.getMergedSources().forEach(order -> sources.putIfAbsent(order.getId(), order));
    }
    return sources.values().stream()
        .sorted(
            Comparator.comparing(
                    OrderEntity::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(OrderEntity::getId))
        .map(
            source ->
                new MergedOrderSource()
                    .orderId(source.getId())
                    .orderDate(source.getOrderDate())
                    .scrap(source.getScrap()))
        .toList();
  }

  /**
   * Leaves lines folded into a merged line off the order. The merged line stands in for them on
   * the sheet; listing both would count the same goods twice.
   */
  @AfterMapping
  default void dropMergedAwayLines(@MappingTarget Order target, OrderEntity entity) {
    if (target.getOrderItems() == null || entity.getOrderItems() == null) {
      return;
    }
    Set<Long> hidden =
        entity.getOrderItems().stream()
            .filter(OrderItemEntity::isMergedAway)
            .map(OrderItemEntity::getId)
            .collect(Collectors.toSet());
    if (!hidden.isEmpty()) {
      target.setOrderItems(
          new ArrayList<>(
              target.getOrderItems().stream().filter(line -> !hidden.contains(line.getId())).toList()));
    }
  }

  @AfterMapping
  default void setOrderBackRef(@MappingTarget OrderEntity entity, NewOrder source) {
    // Set order back-reference on each item (party is set by OrderServiceImpl.afterCreate)
    if (entity.getOrderItems() != null) {
      for (OrderItemEntity item : entity.getOrderItems()) {
        item.setOrder(entity);
      }
    }
  }
}

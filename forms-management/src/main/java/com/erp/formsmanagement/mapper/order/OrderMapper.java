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
import java.util.List;
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
  @Mapping(target = "mergedInto", ignore = true)
  @Mapping(target = "mergedSources", ignore = true)
  @Mapping(target = "orderItems", source = "items")
  OrderEntity toEntity(NewOrder newOrder);

  /**
   * The scrap is deliberately not updated here. Editing an order means resending every line, and
   * the sheet that does so has no reason to know the agreed scrap — leaving it mapped meant any
   * ordinary edit silently cleared it. It is set once on create and changed through its own
   * endpoint after that.
   */
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "scrap", ignore = true)
  @Mapping(target = "mergedInto", ignore = true)
  @Mapping(target = "mergedSources", ignore = true)
  @Mapping(target = "orderItems", source = "items")
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
   * DISPATCHED means every line has gone in full. Anything in between is IN_JOB_WORK.
   */
  default OrderStatus toStatus(OrderEntity entity) {
    List<OrderItemEntity> items = entity.getOrderItems();
    if (items == null || items.isEmpty()) {
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
      if (ordered <= 0 || dispatched < ordered) {
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
    List<OrderEntity> sources = entity.getMergedSources();
    if (sources == null || sources.isEmpty()) {
      return List.of();
    }
    return sources.stream()
        .map(
            source ->
                new MergedOrderSource()
                    .orderId(source.getId())
                    .orderDate(source.getOrderDate())
                    .scrap(source.getScrap()))
        .toList();
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

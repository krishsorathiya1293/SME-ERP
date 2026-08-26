package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.NewOrderItem;
import com.erp.api.ordermanagement.model.OrderItem;
import com.erp.api.ordermanagement.model.OrderItemSize;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkOrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.mapper.EntityMapper;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderItemMapper extends EntityMapper<OrderItemEntity, NewOrderItem, OrderItem> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "order", ignore = true)
  @Mapping(target = "itemSize", ignore = true)
  @Mapping(target = "mergedIntoItem", ignore = true)
  @Mapping(target = "mergedSourceItems", ignore = true)
  @Mapping(target = "completed", ignore = true)
  OrderItemEntity toEntity(NewOrderItem newOrderItem);

  // Ignored on update for the same reason scrap and the order's own tick are: editing an order
  // resends every line, and leaving this mapped means an ordinary edit silently un-ticks a line
  // somebody had already finished with.
  @Mapping(target = "order", ignore = true)
  @Mapping(target = "itemSize", ignore = true)
  @Mapping(target = "mergedIntoItem", ignore = true)
  @Mapping(target = "mergedSourceItems", ignore = true)
  @Mapping(target = "completed", ignore = true)
  void updateEntity(@MappingTarget OrderItemEntity entity, NewOrderItem newOrderItem);

  @Mapping(target = "itemSize", expression = "java(toItemSize(entity))")
  @Mapping(target = "mergedFromItemIds", expression = "java(toMergedFromItemIds(entity))")
  @Mapping(target = "jobWorkSentPc", expression = "java(toJobWorkSentPc(entity))")
  OrderItem toDomain(OrderItemEntity entity);

  List<OrderItem> toDomainList(List<OrderItemEntity> entities);

  default OrderItemSize toItemSize(OrderItemEntity entity) {
    return SizeContextMapper.toOrderItemSize(entity.getItemSize());
  }

  /**
   * The lines this one sums, but only when it genuinely sums more than one.
   *
   * <p>A merge carries every line of every source order across; most of them have no counterpart
   * and simply ride along unchanged. Reporting those as "merged" would put the marker on lines
   * nobody added together, so a single source reads the same as none.
   */
  default List<Long> toMergedFromItemIds(OrderItemEntity entity) {
    List<OrderItemEntity> sources = entity.getMergedSourceItems();
    if (sources == null || sources.size() < 2) {
      return List.of();
    }
    return sources.stream().map(OrderItemEntity::getId).toList();
  }

  /**
   * Pieces of this line already out on a chitthi, summed across every batch.
   *
   * <p>The order sheet had only {@code jobActionDone} — a yes/no that says something left, and
   * nothing about how much. A 200 pc line with 100 sent looks identical to one sent in full, so
   * the other 100 quietly never go. This is the figure that tells them apart.
   *
   * <p>A chitthi covering several lines is counted by this line's own allocation rather than the
   * batch total; a plain one is counted whole. The primary line of a merged chitthi reaches it
   * both ways, so the allocation wins and it is never counted twice.
   */
  default Double toJobWorkSentPc(OrderItemEntity entity) {
    double sent = 0d;

    List<JobWorkEntity> own = entity.getJobWorks();
    if (own != null) {
      for (JobWorkEntity jobWork : own) {
        List<JobWorkOrderItemEntity> allocations = jobWork.getMergedOrderItems();
        boolean split = allocations != null && !allocations.isEmpty();
        if (!split && jobWork.getQtyPc() != null) {
          sent += jobWork.getQtyPc();
        }
      }
    }

    List<JobWorkOrderItemEntity> shares = entity.getJobWorkAllocations();
    if (shares != null) {
      for (JobWorkOrderItemEntity share : shares) {
        if (share.getQtyPc() != null) {
          sent += share.getQtyPc();
        }
      }
    }

    return sent;
  }

  @AfterMapping
  default void setItemSizeRef(@MappingTarget OrderItemEntity entity, NewOrderItem source) {
    if (source.getItemSizeId() != null) {
      ItemBlueprintDataEntity itemSize = new ItemBlueprintDataEntity();
      itemSize.setId(source.getItemSizeId());
      entity.setItemSize(itemSize);
    }
  }
}

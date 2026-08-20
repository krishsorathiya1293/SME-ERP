package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.NewOrderItem;
import com.erp.api.ordermanagement.model.OrderItem;
import com.erp.api.ordermanagement.model.OrderItemSize;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
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
  OrderItemEntity toEntity(NewOrderItem newOrderItem);

  @Mapping(target = "order", ignore = true)
  @Mapping(target = "itemSize", ignore = true)
  @Mapping(target = "mergedIntoItem", ignore = true)
  @Mapping(target = "mergedSourceItems", ignore = true)
  void updateEntity(@MappingTarget OrderItemEntity entity, NewOrderItem newOrderItem);

  @Mapping(target = "itemSize", expression = "java(toItemSize(entity))")
  @Mapping(target = "mergedFromItemIds", expression = "java(toMergedFromItemIds(entity))")
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

  @AfterMapping
  default void setItemSizeRef(@MappingTarget OrderItemEntity entity, NewOrderItem source) {
    if (source.getItemSizeId() != null) {
      ItemBlueprintDataEntity itemSize = new ItemBlueprintDataEntity();
      itemSize.setId(source.getItemSizeId());
      entity.setItemSize(itemSize);
    }
  }
}

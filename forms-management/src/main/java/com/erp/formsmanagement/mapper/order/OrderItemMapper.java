package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.NewOrderItem;
import com.erp.api.ordermanagement.model.OrderItem;
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
  OrderItemEntity toEntity(NewOrderItem newOrderItem);

  @Mapping(target = "order", ignore = true)
  @Mapping(target = "itemSize", ignore = true)
  void updateEntity(@MappingTarget OrderItemEntity entity, NewOrderItem newOrderItem);

  OrderItem toDomain(OrderItemEntity entity);

  List<OrderItem> toDomainList(List<OrderItemEntity> entities);

  @AfterMapping
  default void setItemSizeRef(@MappingTarget OrderItemEntity entity, NewOrderItem source) {
    if (source.getItemSizeId() != null) {
      ItemBlueprintDataEntity itemSize = new ItemBlueprintDataEntity();
      itemSize.setId(source.getItemSizeId());
      entity.setItemSize(itemSize);
    }
  }
}

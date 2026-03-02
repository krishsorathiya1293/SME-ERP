package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
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
  @Mapping(target = "orderItems", source = "items")
  OrderEntity toEntity(NewOrder newOrder);

  @Mapping(target = "party", ignore = true)
  @Mapping(target = "orderItems", source = "items")
  void updateEntity(@MappingTarget OrderEntity entity, NewOrder newOrder);

  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toLocalDate")
  Order toDomain(OrderEntity entity);

  //  @Mapping(target = "partyName", source = "party.name")
  //  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toLocalDate")
  //  @Mapping(
  //      target = "totalItems",
  //      expression = "java(entity.getOrderItems() == null ? 0 : entity.getOrderItems().size())")
  //  OrderInfo toInfo(OrderEntity entity);

  List<Order> toDomainList(List<OrderEntity> entities);

  @Named("toLocalDate")
  default LocalDate toLocalDate(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.toLocalDate();
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

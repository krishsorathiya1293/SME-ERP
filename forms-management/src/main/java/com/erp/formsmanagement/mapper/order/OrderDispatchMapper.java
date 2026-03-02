package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.NewOrderDispatch;
import com.erp.api.ordermanagement.model.OrderDispatch;
import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.mapper.EntityMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderDispatchMapper
    extends EntityMapper<OrderDispatchEntity, NewOrderDispatch, OrderDispatch> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "orderItem", ignore = true)
  OrderDispatchEntity toEntity(NewOrderDispatch newOrderDispatch);

  @Mapping(target = "orderItem", ignore = true)
  void updateEntity(@MappingTarget OrderDispatchEntity entity, NewOrderDispatch newOrderDispatch);

  OrderDispatch toDomain(OrderDispatchEntity entity);

  List<OrderDispatch> toDomainList(List<OrderDispatchEntity> entities);
}

package com.erp.formsmanagement.mapper.master;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper extends EntityMapper<ItemEntity, NewItem, Item> {
  @Mapping(source = "size.id", target = "sizeId")
  @Mapping(source = "size.sizeInInch", target = "sizeInInch")
  @Mapping(source = "size.sizeInMm", target = "sizeInMm")
  @Mapping(source = "size.dozenWeight", target = "dozenWeight")
  Item toDomain(ItemEntity entity);

  @Mapping(target = "size", ignore = true)
  ItemEntity toEntity(NewItem newItem);

  @Mapping(target = "size", ignore = true)
  void updateEntity(@MappingTarget ItemEntity entity, NewItem newItem);
}

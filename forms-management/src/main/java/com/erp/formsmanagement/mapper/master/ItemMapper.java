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
  @Mapping(source = "category", target = "itemCategory")
  @Mapping(source = "subCategory", target = "itemSubCategory")
  Item toDomain(ItemEntity entity);

  ItemEntity toEntity(NewItem newItem);

  void updateEntity(@MappingTarget ItemEntity entity, NewItem newItem);
}

package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.mastermanagement.domain.ItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {
  @Mapping(source = "category", target = "itemCategory")
  @Mapping(source = "subCategory", target = "itemSubCategory")
  Item toDomain(ItemEntity entity);

  ItemEntity toEntity(NewItem newItem);

  void updateEntity(@MappingTarget ItemEntity entity, NewItem newItem);
}

package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.mastermanagement.domain.ItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "subCategoryId", source = "subCategory.id")
    Item toDomain(ItemEntity entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    ItemEntity toEntity(NewItem newItem);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    void updateEntity(@MappingTarget ItemEntity entity, NewItem newItem);
}

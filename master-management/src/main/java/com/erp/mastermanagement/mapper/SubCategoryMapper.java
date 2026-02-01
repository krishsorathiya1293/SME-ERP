package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.mastermanagement.domain.SubCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubCategoryMapper {
    @Mapping(target = "categoryId", source = "category.id")
    SubCategory toDomain(SubCategoryEntity entity);

    @Mapping(target = "category", ignore = true)
    SubCategoryEntity toEntity(NewSubCategory newSubCategory);

    @Mapping(target = "category", ignore = true)
    void updateEntity(@MappingTarget SubCategoryEntity entity, NewSubCategory newSubCategory);
}

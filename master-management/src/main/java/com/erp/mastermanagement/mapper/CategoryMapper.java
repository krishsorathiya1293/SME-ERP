package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.mastermanagement.domain.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toDomain(CategoryEntity entity);

    CategoryEntity toEntity(NewCategory newCategory);

    void updateEntity(@MappingTarget CategoryEntity entity, NewCategory newCategory);
}

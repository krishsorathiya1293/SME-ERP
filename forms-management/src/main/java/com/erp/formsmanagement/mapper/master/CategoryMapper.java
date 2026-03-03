package com.erp.formsmanagement.mapper.master;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.formsmanagement.domain.entity.master.CategoryEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends EntityMapper<CategoryEntity, NewCategory, Category> {
  Category toDomain(CategoryEntity entity);

  CategoryEntity toEntity(NewCategory newCategory);

  CategoryEntity toEntity(Category category);

  void updateEntity(@MappingTarget CategoryEntity entity, NewCategory newCategory);
}

package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.mastermanagement.domain.CategoryEntity;
import com.erp.mastermanagement.domain.SubCategoryEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
  Category toDomain(CategoryEntity entity);

  CategoryEntity toEntity(NewCategory newCategory);

  CategoryEntity toEntity(Category category);

  void updateEntity(@MappingTarget CategoryEntity entity, NewCategory newCategory);

  @AfterMapping
  default void linkSubCategories(@MappingTarget CategoryEntity category) {
    if (category.getSubCategories() == null) {
      return;
    }

    for (SubCategoryEntity sub : category.getSubCategories()) {
      sub.setCategory(category);
    }
  }
}

package com.erp.formsmanagement.mapper.master;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.formsmanagement.domain.entity.master.SubCategoryEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubCategoryMapper extends EntityMapper<SubCategoryEntity, NewSubCategory, SubCategory> {
  SubCategory toDomain(SubCategoryEntity entity);

  @Mapping(target = "category", ignore = true)
  SubCategoryEntity toEntity(NewSubCategory newSubCategory);

  @Mapping(target = "category", ignore = true)
  SubCategoryEntity toEntity(SubCategory subCategory);

  @Mapping(target = "category", ignore = true)
  void updateEntity(@MappingTarget SubCategoryEntity entity, NewSubCategory newSubCategory);
}

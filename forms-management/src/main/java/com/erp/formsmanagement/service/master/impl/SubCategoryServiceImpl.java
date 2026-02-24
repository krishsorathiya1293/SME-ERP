package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.SubCategoryEntity;
import com.erp.formsmanagement.domain.repository.master.CategoryRepository;
import com.erp.formsmanagement.domain.repository.master.SubCategoryRepository;
import com.erp.formsmanagement.mapper.master.SubCategoryMapper;
import com.erp.formsmanagement.service.master.SubCategoryService;
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV2;
import com.erp.util.GetAllQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl
    extends AbstractCrudServiceV2<SubCategoryEntity, NewSubCategory, SubCategory, Long>
    implements SubCategoryService {

  private final SubCategoryRepository subCategoryRepository;
  private final CategoryRepository categoryRepository;
  private final SubCategoryMapper subCategoryMapper;

  @Override
  protected JpaRepository<SubCategoryEntity, Long> repository() {
    return subCategoryRepository;
  }

  @Override
  protected EntityMapper<SubCategoryEntity, NewSubCategory, SubCategory> mapper() {
    return subCategoryMapper;
  }

  @Override
  protected void afterCreate(SubCategoryEntity entity, Long categoryId, NewSubCategory request) {
    entity.setCategory(
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, categoryId))));
  }

  @Override
  protected void afterUpdate(SubCategoryEntity entity, Long categoryId, NewSubCategory request) {
    afterCreate(entity, categoryId, request);
  }

  @Override
  public List<SubCategory> getAll(Long categoryId, GetAllQuery<Void> query) {
    return subCategoryRepository
        .findAll(subCategoryRepository.byCategoryAndName(categoryId, query.search().orElse(null)))
        .stream()
        .map(subCategoryMapper::toDomain)
        .toList();
  }
}

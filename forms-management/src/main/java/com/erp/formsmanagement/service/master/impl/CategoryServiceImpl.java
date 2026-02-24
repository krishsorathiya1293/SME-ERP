package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.formsmanagement.domain.entity.master.CategoryEntity;
import com.erp.formsmanagement.domain.repository.master.CategoryRepository;
import com.erp.formsmanagement.mapper.master.CategoryMapper;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV1;
import com.erp.util.GetAllQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl
    extends AbstractCrudServiceV1<CategoryEntity, NewCategory, Category>
    implements CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Override
  protected JpaRepository<CategoryEntity, Long> repository() {
    return categoryRepository;
  }

  @Override
  protected EntityMapper<CategoryEntity, NewCategory, Category> mapper() {
    return categoryMapper;
  }

  @Override
  public List<Category> getAll(GetAllQuery<Void> query) {
    return query.search()
        .filter(s -> !s.isBlank())
        .map(categoryRepository::searchByCategoryOrSubCategory)
        .orElseGet(categoryRepository::findAll)
        .stream()
        .map(categoryMapper::toDomain)
        .toList();
  }
}

package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.formsmanagement.domain.entity.master.CategoryEntity;
import com.erp.formsmanagement.domain.repository.master.CategoryRepository;
import com.erp.formsmanagement.mapper.master.CategoryMapper;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl
    extends AbstractSpecificationServiceV1<CategoryEntity, NewCategory, Category>
    implements CategoryService {

  private final CategoryRepository categoryRepository;

  public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
    super(categoryRepository, categoryMapper);
    this.categoryRepository = categoryRepository;
  }

  @Override
  public List<Category> getAll(GetAllQuery<Void> query) {
    return query
        .search()
        .filter(s -> !s.isBlank())
        .map(categoryRepository::searchByCategoryOrSubCategory)
        .orElseGet(categoryRepository::findAll)
        .stream()
        .map(e -> mapper().toDomain(e))
        .toList();
  }
}

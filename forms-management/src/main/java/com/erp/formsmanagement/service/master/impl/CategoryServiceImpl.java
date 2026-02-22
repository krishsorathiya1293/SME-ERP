package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.repository.master.CategoryRepository;
import com.erp.formsmanagement.mapper.master.CategoryMapper;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.util.GetAllQuery;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Override
  public List<Category> getAll(GetAllQuery<Void> query) {
    return query
        .search()
        .filter(s -> !s.isBlank())
        .map(categoryRepository::searchByCategoryOrSubCategory)
        .orElseGet(categoryRepository::findAll)
        .stream()
        .map(categoryMapper::toDomain)
        .toList();
  }

  @Override
  public Category getById(Long id) {
    return categoryRepository
        .findById(id)
        .map(categoryMapper::toDomain)
        .orElseThrow(
            () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
  }

  @Override
  public CreateResult<Category> save(NewCategory request) {
    return new CreateOne<>(
        categoryMapper.toDomain(categoryRepository.save(categoryMapper.toEntity(request))));
  }

  @Override
  public Category update(Long id, NewCategory request) {
    var entity =
        categoryRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    categoryMapper.updateEntity(entity, request);
    return categoryMapper.toDomain(categoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    categoryRepository.deleteById(id);
  }
}

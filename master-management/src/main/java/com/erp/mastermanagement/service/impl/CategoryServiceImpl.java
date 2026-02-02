package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.mastermanagement.mapper.CategoryMapper;
import com.erp.mastermanagement.repository.CategoryRepository;
import com.erp.mastermanagement.service.CategoryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Override
  public List<Category> getAllbyId(Optional<String> search) {
    return search
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
  public Category save(NewCategory request) {
    return categoryMapper.toDomain(categoryRepository.save(categoryMapper.toEntity(request)));
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

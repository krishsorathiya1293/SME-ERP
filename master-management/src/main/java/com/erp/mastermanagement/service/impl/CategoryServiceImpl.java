package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
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
    if (search.isPresent()) {
      return categoryRepository
          .findAll((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.get().toLowerCase() + "%"))
          .stream()
          .map(categoryMapper::toDomain)
          .toList();
    }
    return categoryRepository.findAll().stream().map(categoryMapper::toDomain).toList();
  }

  @Override
  public Category getById(Long id) {
    return categoryRepository
        .findById(id)
        .map(categoryMapper::toDomain)
        .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
  }

  @Override
  public Category save(NewCategory request) {
    var entity = categoryMapper.toEntity(request);
    return categoryMapper.toDomain(categoryRepository.save(entity));
  }

  @Override
  public Category update(Long id, NewCategory request) {
    var entity = categoryRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    categoryMapper.updateEntity(entity, request);
    return categoryMapper.toDomain(categoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    categoryRepository.deleteById(id);
  }
}

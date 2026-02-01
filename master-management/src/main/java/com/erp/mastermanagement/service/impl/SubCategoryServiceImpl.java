package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.mastermanagement.mapper.SubCategoryMapper;
import com.erp.mastermanagement.repository.CategoryRepository;
import com.erp.mastermanagement.repository.SubCategoryRepository;
import com.erp.mastermanagement.service.SubCategoryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl implements SubCategoryService {
  private final SubCategoryRepository subCategoryRepository;
  private final CategoryRepository categoryRepository;
  private final SubCategoryMapper subCategoryMapper;

  @Override
  public List<SubCategory> getAll(Optional<String> search) {
    if (search.isPresent()) {
      return subCategoryRepository
          .findAll((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.get().toLowerCase() + "%"))
          .stream()
          .map(subCategoryMapper::toDomain)
          .toList();
    }
    return subCategoryRepository.findAll().stream().map(subCategoryMapper::toDomain).toList();
  }

  @Override
  public SubCategory getById(Long id) {
    return subCategoryRepository
        .findById(id)
        .map(subCategoryMapper::toDomain)
        .orElseThrow(() -> new RuntimeException("SubCategory not found with id: " + id));
  }

  @Override
  public SubCategory save(NewSubCategory request) {
    var category = categoryRepository
        .findById(request.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
    var entity = subCategoryMapper.toEntity(request);
    entity.setCategory(category);
    return subCategoryMapper.toDomain(subCategoryRepository.save(entity));
  }

  @Override
  public SubCategory update(Long id, NewSubCategory request) {
    var entity = subCategoryRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("SubCategory not found with id: " + id));
    var category = categoryRepository
        .findById(request.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

    subCategoryMapper.updateEntity(entity, request);
    entity.setCategory(category);
    return subCategoryMapper.toDomain(subCategoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    subCategoryRepository.deleteById(id);
  }
}

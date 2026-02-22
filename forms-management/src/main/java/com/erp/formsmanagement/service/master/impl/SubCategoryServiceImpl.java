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
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
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
  public List<SubCategory> getAll(Long categoryId, Optional<String> search) {
    return subCategoryRepository
        .findAll(subCategoryRepository.byCategoryAndName(categoryId, search.orElse(null)))
        .stream()
        .map(subCategoryMapper::toDomain)
        .toList();
  }

  @Override
  public SubCategory getById(Long categoryId, Long id) {
    return subCategoryRepository
        .findById(id)
        .map(subCategoryMapper::toDomain)
        .orElseThrow(
            () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
  }

  @Override
  public CreateResult<SubCategory> save(Long categoryId, NewSubCategory request) {
    SubCategoryEntity entity = subCategoryMapper.toEntity(request);
    entity.setCategory(
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, categoryId))));
    return new CreateOne<>(subCategoryMapper.toDomain(subCategoryRepository.save(entity)));
  }

  @Override
  public SubCategory update(Long categoryId, Long id, NewSubCategory request) {
    SubCategoryEntity entity =
        subCategoryRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));

    subCategoryMapper.updateEntity(entity, request);
    entity.setCategory(
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id))));
    return subCategoryMapper.toDomain(subCategoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long categoryId, Long id) {
    subCategoryRepository.deleteById(id);
  }
}

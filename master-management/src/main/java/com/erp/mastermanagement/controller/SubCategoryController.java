package com.erp.mastermanagement.controller;

import com.erp.api.mastermanagement.SubCategoryMasterManagementApi;
import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.mastermanagement.service.SubCategoryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubCategoryController implements SubCategoryMasterManagementApi {

  private final SubCategoryService subCategoryService;

  @Override
  public ResponseEntity<SubCategory> createSubCategory(
      Long categoryId, NewSubCategory newSubCategory) {
    return ResponseEntity.ok(subCategoryService.save(categoryId, newSubCategory));
  }

  @Override
  public ResponseEntity<Void> deleteSubCategory(Long categoryId, Long id) {
    subCategoryService.deleteById(categoryId, id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<SubCategory>> getAllSubCategories(
      Long categoryId, Optional<String> search) {
    return ResponseEntity.ok(subCategoryService.getAll(categoryId, search));
  }

  @Override
  public ResponseEntity<SubCategory> getSubCategoryById(Long categoryId, Long id) {
    return ResponseEntity.ok(subCategoryService.getById(categoryId, id));
  }

  @Override
  public ResponseEntity<SubCategory> updateSubCategory(
      Long categoryId, Long id, NewSubCategory newSubCategory) {
    return ResponseEntity.ok(subCategoryService.update(categoryId, id, newSubCategory));
  }
}

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
  public ResponseEntity<SubCategory> createSubCategory(NewSubCategory newSubCategory) {
    return ResponseEntity.ok(subCategoryService.save(newSubCategory));
  }

  @Override
  public ResponseEntity<Void> deleteSubCategory(Long id) {
    subCategoryService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<SubCategory>> getAllSubCategories(Optional<String> search) {
    return ResponseEntity.ok(subCategoryService.getAll(search));
  }

  @Override
  public ResponseEntity<SubCategory> getSubCategoryById(Long id) {
    return ResponseEntity.ok(subCategoryService.getById(id));
  }

  @Override
  public ResponseEntity<SubCategory> updateSubCategory(Long id, NewSubCategory newSubCategory) {
    return ResponseEntity.ok(subCategoryService.update(id, newSubCategory));
  }
}

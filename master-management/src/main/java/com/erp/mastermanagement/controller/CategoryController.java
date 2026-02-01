package com.erp.mastermanagement.controller;

import com.erp.api.mastermanagement.CategoryMasterManagementApi;
import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.mastermanagement.service.CategoryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryMasterManagementApi {
  private final CategoryService categoryService;

  @Override
  public ResponseEntity<Category> createCategory(NewCategory newCategory) {
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.save(newCategory));
  }

  @Override
  public ResponseEntity<Void> deleteCategory(Long id) {
    categoryService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<Category>> getAllCategories(Optional<String> search) {
    return ResponseEntity.ok(categoryService.getAllbyId(search));
  }

  @Override
  public ResponseEntity<Category> getCategoryById(Long id) {
    return ResponseEntity.ok(categoryService.getById(id));
  }

  @Override
  public ResponseEntity<Category> updateCategory(Long id, NewCategory newCategory) {
    return ResponseEntity.ok(categoryService.update(id, newCategory));
  }
}

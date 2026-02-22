package com.erp.formsmanagement.controller.master;

import com.erp.api.mastermanagement.CategoryMasterManagementApi;
import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.controller.GenericCrudDelegateV1;
import com.erp.controller.GetAllDelegateV1;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryMasterManagementApi {

  private final CategoryService categoryService;

  private GenericCrudDelegateV1<NewCategory, Category, Long> crud() {
    return new GenericCrudDelegateV1<>(categoryService);
  }

  private GetAllDelegateV1<Void, List<Category>> page() {
    return new GetAllDelegateV1<>(categoryService);
  }

  @Override
  public ResponseEntity<Category> createCategory(NewCategory newCategory) {
    return crud().createOne(newCategory);
  }

  @Override
  public ResponseEntity<Void> deleteCategory(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<List<Category>> getAllCategories(Optional<String> search) {
    return page().getAll(GetAllQuery.of(search));
  }

  @Override
  public ResponseEntity<Category> getCategoryById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<Category> updateCategory(Long id, NewCategory newCategory) {
    return crud().update(id, newCategory);
  }
}

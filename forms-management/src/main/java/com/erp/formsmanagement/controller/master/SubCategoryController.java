package com.erp.formsmanagement.controller.master;

import com.erp.api.mastermanagement.SubCategoryMasterManagementApi;
import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.controller.GenericCrudDelegateV2;
import com.erp.controller.GetAllDelegateV2;
import com.erp.formsmanagement.service.master.SubCategoryService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubCategoryController implements SubCategoryMasterManagementApi {

  private final SubCategoryService subCategoryService;

  private GenericCrudDelegateV2<Long, NewSubCategory, SubCategory, Long> crud() {
    return new GenericCrudDelegateV2<>(subCategoryService);
  }

  private GetAllDelegateV2<Long, Void, List<SubCategory>> page() {
    return new GetAllDelegateV2<>(subCategoryService);
  }

  @Override
  public ResponseEntity<SubCategory> createSubCategory(
      Long categoryId, NewSubCategory newSubCategory) {

    return crud().createOne(categoryId, newSubCategory);
  }

  @Override
  public ResponseEntity<Void> deleteSubCategory(Long categoryId, Long id) {
    return crud().delete(categoryId, id);
  }

  @Override
  public ResponseEntity<List<SubCategory>> getAllSubCategories(
      Long categoryId, Optional<String> search) {

    return page().getAll(categoryId, GetAllQuery.of(search));
  }

  @Override
  public ResponseEntity<SubCategory> getSubCategoryById(Long categoryId, Long id) {
    return crud().getById(categoryId, id);
  }

  @Override
  public ResponseEntity<SubCategory> updateSubCategory(
      Long categoryId, Long id, NewSubCategory newSubCategory) {

    return crud().update(categoryId, id, newSubCategory);
  }
}

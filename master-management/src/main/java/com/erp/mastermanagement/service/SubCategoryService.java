package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.service.CoreService;
import java.util.List;
import java.util.Optional;

public interface SubCategoryService extends CoreService<NewSubCategory, SubCategory, Long> {
  List<SubCategory> getAll(Optional<String> search);
}

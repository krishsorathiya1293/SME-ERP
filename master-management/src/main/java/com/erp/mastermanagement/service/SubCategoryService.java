package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.service.CoreServiceV2;
import java.util.List;
import java.util.Optional;

public interface SubCategoryService extends CoreServiceV2<Long, NewSubCategory, SubCategory, Long> {
  List<SubCategory> getAll(Long categoryId, Optional<String> search);
}

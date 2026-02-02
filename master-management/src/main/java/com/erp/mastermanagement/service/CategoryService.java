package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.service.CoreServiceV1;
import java.util.List;
import java.util.Optional;

public interface CategoryService extends CoreServiceV1<NewCategory, Category, Long> {
  List<Category> getAllbyId(Optional<String> search);
}

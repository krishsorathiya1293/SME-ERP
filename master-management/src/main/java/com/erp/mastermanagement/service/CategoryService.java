package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.service.CoreService;
import java.util.List;
import java.util.Optional;

public interface CategoryService extends CoreService<NewCategory, Category, Long> {
  List<Category> getAllbyId(Optional<String> search);
}

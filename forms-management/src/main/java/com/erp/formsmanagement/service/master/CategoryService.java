package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.Category;
import com.erp.api.mastermanagement.model.NewCategory;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;
import java.util.List;

public interface CategoryService
    extends CoreServiceV1<NewCategory, Category, Long>, GetAllServiceV1<Void, List<Category>> {}

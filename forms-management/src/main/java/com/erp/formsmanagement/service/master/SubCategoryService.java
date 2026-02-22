package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.NewSubCategory;
import com.erp.api.mastermanagement.model.SubCategory;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import java.util.List;

public interface SubCategoryService
    extends CoreServiceV2<Long, NewSubCategory, SubCategory, Long>,
        GetAllServiceV2<Long, Void, List<SubCategory>> {}

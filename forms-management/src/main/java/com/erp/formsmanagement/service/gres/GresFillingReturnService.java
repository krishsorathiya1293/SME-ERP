package com.erp.formsmanagement.service.gres;

import com.erp.api.gresmanagement.model.GresFillingReturn;
import com.erp.api.gresmanagement.model.NewGresFillingReturn;
import com.erp.api.gresmanagement.model.PaginatedResultGresFillingReturn;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public interface GresFillingReturnService
    extends CoreServiceV2<Long, NewGresFillingReturn, GresFillingReturn, Long>,
        GetAllServiceV2<Long, Void, PaginatedResultGresFillingReturn> {}

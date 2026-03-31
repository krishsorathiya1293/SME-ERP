package com.erp.formsmanagement.service.gres;

import com.erp.api.gresmanagement.model.GresFilling;
import com.erp.api.gresmanagement.model.NewGresFilling;
import com.erp.api.gresmanagement.model.PaginatedResultGresFilling;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public interface GresFillingService
    extends CoreServiceV1<NewGresFilling, GresFilling, Long>,
        GetAllServiceV1<Void, PaginatedResultGresFilling> {}

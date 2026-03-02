package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.NewOrderDispatch;
import com.erp.api.ordermanagement.model.OrderDispatch;
import com.erp.api.ordermanagement.model.PaginatedResultOrderDispatch;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public interface OrderDispatchService
    extends CoreServiceV2<Long, NewOrderDispatch, OrderDispatch, Long>,
        GetAllServiceV2<Long, String, PaginatedResultOrderDispatch> {}

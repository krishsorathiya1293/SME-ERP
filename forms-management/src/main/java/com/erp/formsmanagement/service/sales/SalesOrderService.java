package com.erp.formsmanagement.service.sales;

import com.erp.api.salesmanagement.model.NewSalesOrder;
import com.erp.api.salesmanagement.model.PaginatedResultSalesOrder;
import com.erp.api.salesmanagement.model.SalesOrder;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public interface SalesOrderService
    extends CoreServiceV1<NewSalesOrder, SalesOrder, Long>,
        GetAllServiceV1<String, PaginatedResultSalesOrder> {}

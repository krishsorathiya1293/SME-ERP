package com.erp.formsmanagement.service.purchase;

import com.erp.api.purchasemanagement.model.NewPurchaseOrder;
import com.erp.api.purchasemanagement.model.PaginatedResultPurchaseOrder;
import com.erp.api.purchasemanagement.model.PurchaseOrder;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public interface PurchaseOrderService
    extends CoreServiceV1<NewPurchaseOrder, PurchaseOrder, Long>,
        GetAllServiceV1<String, PaginatedResultPurchaseOrder> {}

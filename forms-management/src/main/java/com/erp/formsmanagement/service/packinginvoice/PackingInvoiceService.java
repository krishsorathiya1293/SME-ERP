package com.erp.formsmanagement.service.packinginvoice;

import com.erp.api.packinginvoicemanagement.model.NewPackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PaginatedResultPackingInvoice;
import com.erp.formsmanagement.domain.entity.packinginvoice.filter.PackingInvoiceFilter;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public interface PackingInvoiceService
    extends CoreServiceV1<NewPackingInvoice, PackingInvoice, Long>,
        GetAllServiceV1<PackingInvoiceFilter, PaginatedResultPackingInvoice> {}

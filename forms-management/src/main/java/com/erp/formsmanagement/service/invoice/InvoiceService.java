package com.erp.formsmanagement.service.invoice;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.service.CoreServiceV1;
import java.util.Optional;

public interface InvoiceService extends CoreServiceV1<NewInvoice, Invoice, Long> {

  Invoice getInvoiceByType(Long id, String invoiceType);

  PaginatedResultInvoice getAll(
      Optional<String> filterByType,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction);
}

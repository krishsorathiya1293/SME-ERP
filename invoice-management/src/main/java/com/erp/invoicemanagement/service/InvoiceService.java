package com.erp.invoicemanagement.service;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.service.CoreServiceV1;
import java.util.List;
import java.util.Optional;

public interface InvoiceService extends CoreServiceV1<NewInvoice, Invoice, Long> {
  List<Invoice> saveInvoice(NewInvoice invoice);

  Invoice getInvoiceByType(Long id, InvoiceType invoiceType);

  PaginatedResultInvoice getAll(
      Optional<String> filterByType,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction);
}

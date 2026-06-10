package com.erp.exportmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.PaginatedResultClientInvoice;
import java.util.Optional;

public interface ClientInvoiceService {

  PaginatedResultClientInvoice getMyInvoices(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection);

  ClientInvoicePdfResult getMyInvoicePdf(Long id);
}

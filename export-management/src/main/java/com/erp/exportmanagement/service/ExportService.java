package com.erp.exportmanagement.service;

import com.erp.api.invoicemanagement.model.Invoice;
import org.springframework.core.io.ByteArrayResource;

public interface ExportService {
  ByteArrayResource generateInvoicePdf(Invoice invoice);

  String shortName(String exporterCompanyName);
}

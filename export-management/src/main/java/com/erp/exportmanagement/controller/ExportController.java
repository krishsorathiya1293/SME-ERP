package com.erp.exportmanagement.controller;

import static com.erp.constant.Constant.INVOICE_FILENAME_FORMAT;

import com.erp.api.exportmanagement.ExportExportManagementApi;
import com.erp.api.exportmanagement.model.InvoiceType;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.exportmanagement.service.ExportService;
import com.erp.invoicemanagement.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExportController implements ExportExportManagementApi {
  private final ExportService exportService;
  private final InvoiceService invoiceService;

  @Override
  public ResponseEntity<Resource> getInvoicePdf(Long id, InvoiceType invoiceType) {
    Invoice invoice = invoiceService.getInvoiceByType(id, String.valueOf(invoiceType));
    ByteArrayResource pdf = exportService.generateInvoicePdf(invoice);
    String filename =
        String.format(
            INVOICE_FILENAME_FORMAT,
            id,
            exportService.shortName(invoice.getExporterCompanyName()),
            invoiceType);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.contentLength())
        .body(pdf);
  }
}

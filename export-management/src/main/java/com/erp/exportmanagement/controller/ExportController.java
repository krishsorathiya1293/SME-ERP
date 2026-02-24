package com.erp.exportmanagement.controller;

import static com.erp.constant.Constant.INVOICE_FILENAME_FORMAT;

import com.erp.api.exportmanagement.ExportExportManagementApi;
import com.erp.api.exportmanagement.model.InvoiceType;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.exportmanagement.service.ExportService;
import com.erp.formsmanagement.service.invoice.InvoiceService;
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
    byte[] pdfBytes = exportService.getCachedPdf("invoice", id, String.valueOf(invoiceType));
    ByteArrayResource pdf = new ByteArrayResource(pdfBytes);
    Invoice invoice = invoiceService.getInvoiceByType(id, String.valueOf(invoiceType));
    String filename =
        String.format(
            INVOICE_FILENAME_FORMAT,
            invoice.getInvoiceNo(),
            exportService.shortName(invoice.getExporterCompanyName()),
            invoiceType.name());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.contentLength())
        .body(pdf);
  }
}

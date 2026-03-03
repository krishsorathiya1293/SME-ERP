package com.erp.exportmanagement.controller;

import static com.erp.constant.Constant.INVOICE_FILENAME_FORMAT;

import com.erp.api.exportmanagement.ExportExportManagementApi;
import com.erp.api.exportmanagement.model.InvoiceType;
import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.controller.AbstractDocumentController;
import com.erp.exportmanagement.service.ExportService;
import com.erp.formsmanagement.service.invoice.InvoiceService;
import com.erp.service.DocumentFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExportController extends AbstractDocumentController
    implements ExportExportManagementApi {
  private final ExportService exportService;
  private final InvoiceService invoiceService;

  @Override
  public ResponseEntity<Resource> getInvoicePdf(Long id, InvoiceType invoiceType) {
    byte[] pdfBytes =
        exportService.getCachedDocument(
            "invoice", id, String.valueOf(invoiceType), DocumentFormat.PDF);
    Invoice invoice = invoiceService.getInvoiceByType(id, String.valueOf(invoiceType));
    String filename =
        String.format(
            INVOICE_FILENAME_FORMAT,
            invoice.getInvoiceNo(),
            exportService.shortName(invoice.getExporterCompanyName()),
            invoiceType.name());

    return buildPdfResponse(pdfBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getJobWorkPng(Long id, JobWorkFormType jobWorkFormType) {
    byte[] pngBytes =
        exportService.getCachedDocument(
            "job-work", id, String.valueOf(jobWorkFormType), DocumentFormat.PNG);
    String filename = "JOBWORK-" + jobWorkFormType + "-" + id + ".png";
    return buildPngResponse(pngBytes, filename);
  }
}

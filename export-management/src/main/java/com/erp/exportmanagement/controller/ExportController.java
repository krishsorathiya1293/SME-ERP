package com.erp.exportmanagement.controller;

import static com.erp.constant.Constant.INVOICE_FILENAME_FORMAT;

import com.erp.api.exportmanagement.ExportExportManagementApi;
import com.erp.api.exportmanagement.model.GresFillingFormType;
import com.erp.api.exportmanagement.model.InvoiceType;
import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.api.exportmanagement.model.PaperSize;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.controller.AbstractDocumentController;
import com.erp.exportmanagement.service.ExportService;
import com.erp.exportmanagement.service.ReportExportService;
import com.erp.formsmanagement.service.invoice.InvoiceService;
import com.erp.service.DocumentFormat;
import java.time.LocalDate;
import java.util.Optional;
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
  private final ReportExportService reportExportService;

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
  public ResponseEntity<Resource> getJobWorkPng(
      Long id, JobWorkFormType jobWorkFormType, Optional<PaperSize> paperSize) {
    PaperSize size = paperSize.orElse(PaperSize.A6);
    byte[] pngBytes =
        exportService.getCachedDocument(
            "job-work", id, jobWorkFormType + "_" + size, DocumentFormat.PNG);
    String filename = "JOBWORK-" + jobWorkFormType + "-" + size + "-" + id + ".png";
    return buildPngResponse(pngBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getPackingInvoicePdf(Long id) {
    byte[] pdfBytes =
        exportService.getCachedDocument("packing-invoice-party", id, "ALL", DocumentFormat.PDF);
    String filename = "PACKING-INVOICE-" + id + ".pdf";
    return buildPdfResponse(pdfBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getGresFillingPng(Long id, GresFillingFormType gresFillingFormType) {
    byte[] pngBytes =
        exportService.getCachedDocument(
            "gres-filling", id, String.valueOf(gresFillingFormType), DocumentFormat.PNG);
    String filename = "GRES-FILLING-" + gresFillingFormType + "-" + id + ".png";
    return buildPngResponse(pngBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getGresFillingReportPdf(
      Long partyId, LocalDate startDate, LocalDate endDate) {
    byte[] pdfBytes = reportExportService.generateGresFillingReportPdf(partyId, startDate, endDate);
    String filename = "GRES-REPORT-" + partyId + ".pdf";
    return buildPdfResponse(pdfBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getJobWorkReportPdf(
      Long partyId, LocalDate startDate, LocalDate endDate) {
    byte[] pdfBytes = reportExportService.generateJobWorkReportPdf(partyId, startDate, endDate);
    String filename = "JOBWORK-REPORT-" + partyId + ".pdf";
    return buildPdfResponse(pdfBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getPurchaseReportPdf(
      Long partyId, LocalDate startDate, LocalDate endDate) {
    byte[] pdfBytes = reportExportService.generatePurchaseReportPdf(partyId, startDate, endDate);
    String filename = "PURCHASE-REPORT-" + partyId + ".pdf";
    return buildPdfResponse(pdfBytes, filename);
  }

  @Override
  public ResponseEntity<Resource> getSalesReportPdf(
      Long partyId, LocalDate startDate, LocalDate endDate) {
    byte[] pdfBytes = reportExportService.generateSalesReportPdf(partyId, startDate, endDate);
    String filename = "SALES-REPORT-" + partyId + ".pdf";
    return buildPdfResponse(pdfBytes, filename);
  }
}

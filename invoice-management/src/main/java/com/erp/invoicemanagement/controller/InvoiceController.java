package com.erp.invoicemanagement.controller;

import com.erp.api.invoicemanagement.InvoiceInvoiceManagementApi;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.invoicemanagement.service.InvoicePdfService;
import com.erp.invoicemanagement.service.InvoiceService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InvoiceController implements InvoiceInvoiceManagementApi {
  private final InvoicePdfService invoicePdfService;
  private final InvoiceService invoiceService;

  @Override
  public ResponseEntity<PaginatedResultInvoice> getAllInvoice(
      Optional<String> filterByType,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(
        invoiceService.getAll(filterByType, search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<List<Invoice>> createInvoice(NewInvoice invoice) {
    return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.saveInvoice(invoice));
  }

  @Override
  public ResponseEntity<Void> deleteInvoice(Long id) {
    invoiceService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Invoice> getInvoiceById(Long id) {
    return ResponseEntity.ok(invoiceService.getById(id));
  }

  @Override
  public ResponseEntity<Resource> getInvoicePdf(Long id, InvoiceType invoiceType) {
    Invoice invoice = invoiceService.getInvoiceByType(id, invoiceType);
    byte[] pdf = invoicePdfService.generatePdf(invoice);

    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData(
        "attachment",
        String.format(
            "invoice_%d_%s_%s.pdf",
            id,
            invoicePdfService.shortName(invoice.getExporterCompanyName()),
            invoiceType));
    headers.setContentLength(pdf.length);

    return new ResponseEntity<>(new org.springframework.core.io.ByteArrayResource(pdf), headers, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Invoice> updateInvoice(Long id, NewInvoice newInvoice) {
    return ResponseEntity.ok(invoiceService.update(id, newInvoice));
  }
}

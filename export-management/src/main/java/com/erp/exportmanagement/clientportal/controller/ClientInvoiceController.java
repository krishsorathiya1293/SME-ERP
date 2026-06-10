package com.erp.exportmanagement.clientportal.controller;

import com.erp.api.clientportalmanagement.ClientInvoicesClientPortalManagementApi;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientInvoice;
import com.erp.controller.AbstractDocumentController;
import com.erp.exportmanagement.clientportal.service.ClientInvoicePdfResult;
import com.erp.exportmanagement.clientportal.service.ClientInvoiceService;
import com.erp.usermanagement.security.roles.Client;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Client
public class ClientInvoiceController extends AbstractDocumentController
    implements ClientInvoicesClientPortalManagementApi {

  private final ClientInvoiceService clientInvoiceService;

  @Override
  public ResponseEntity<PaginatedResultClientInvoice> getMyInvoices(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(
        clientInvoiceService.getMyInvoices(page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Resource> getMyInvoicePdf(Long id) {
    ClientInvoicePdfResult result = clientInvoiceService.getMyInvoicePdf(id);
    return buildPdfResponse(result.bytes(), result.filename());
  }
}

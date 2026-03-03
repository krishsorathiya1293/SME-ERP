package com.erp.formsmanagement.controller.invoice;

import com.erp.api.invoicemanagement.InvoiceInvoiceManagementApi;
import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.invoice.InvoiceService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvoiceController
    extends AbstractCrudControllerV1<NewInvoice, Invoice, String, PaginatedResultInvoice>
    implements InvoiceInvoiceManagementApi {

  public InvoiceController(InvoiceService s) {
    super(s, s);
  }

  @Override
  public ResponseEntity<PaginatedResultInvoice> getAllInvoice(
      Optional<String> filterByType,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page().getAll(GetAllQuery.of(filterByType, search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<List<Invoice>> createInvoice(NewInvoice invoice) {
    return crud().createMany(invoice);
  }

  @Override
  public ResponseEntity<Void> deleteInvoice(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<Invoice> getInvoiceById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<Invoice> updateInvoice(Long id, NewInvoice newInvoice) {
    return crud().update(id, newInvoice);
  }
}

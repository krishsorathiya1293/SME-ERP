package com.erp.formsmanagement.controller.packinginvoice;

import com.erp.api.packinginvoicemanagement.PackingInvoicePackingInvoiceManagementApi;
import com.erp.api.packinginvoicemanagement.model.NewPackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PaginatedResultPackingInvoice;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.domain.entity.packinginvoice.filter.PackingInvoiceFilter;
import com.erp.formsmanagement.service.packinginvoice.PackingInvoiceService;
import com.erp.util.GetAllQuery;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PackingInvoiceController
    extends AbstractCrudControllerV1<
        NewPackingInvoice, PackingInvoice, PackingInvoiceFilter, PaginatedResultPackingInvoice>
    implements PackingInvoicePackingInvoiceManagementApi {

  public PackingInvoiceController(PackingInvoiceService service) {
    super(service, service);
  }

  @Override
  public ResponseEntity<PaginatedResultPackingInvoice> getAllPackingInvoices(
      Optional<Long> partyId,
      Optional<String> month,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {

    PackingInvoiceFilter filter =
        new PackingInvoiceFilter(
            partyId.orElse(null), month.map(YearMonth::parse).orElse(null));

    return page()
        .getAll(GetAllQuery.of(Optional.of(filter), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<PackingInvoice> createPackingInvoice(NewPackingInvoice newPackingInvoice) {
    return crud().createOne(newPackingInvoice);
  }

  @Override
  public ResponseEntity<Void> deletePackingInvoice(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<PackingInvoice> getPackingInvoiceById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<PackingInvoice> updatePackingInvoice(
      Long id, NewPackingInvoice newPackingInvoice) {
    return crud().update(id, newPackingInvoice);
  }
}

package com.erp.formsmanagement.controller.gres;

import com.erp.api.gresmanagement.GresFillingGresManagementApi;
import com.erp.api.gresmanagement.model.GresFilling;
import com.erp.api.gresmanagement.model.NewGresFilling;
import com.erp.api.gresmanagement.model.PaginatedResultGresFilling;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.gres.GresFillingService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GresFillingController
    extends AbstractCrudControllerV1<NewGresFilling, GresFilling, Void, PaginatedResultGresFilling>
    implements GresFillingGresManagementApi {

  public GresFillingController(GresFillingService gresFillingService) {
    super(gresFillingService, gresFillingService);
  }

  @Override
  public ResponseEntity<GresFilling> createGresFilling(NewGresFilling newGresFilling) {
    return crud().createOne(newGresFilling);
  }

  @Override
  public ResponseEntity<Void> deleteGresFilling(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<PaginatedResultGresFilling> getAllGresFillings(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<GresFilling> getGresFillingById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<GresFilling> updateGresFilling(Long id, NewGresFilling newGresFilling) {
    return crud().update(id, newGresFilling);
  }
}

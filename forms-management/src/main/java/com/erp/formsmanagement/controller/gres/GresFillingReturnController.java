package com.erp.formsmanagement.controller.gres;

import com.erp.api.gresmanagement.GresFillingReturnGresManagementApi;
import com.erp.api.gresmanagement.model.GresFillingReturn;
import com.erp.api.gresmanagement.model.NewGresFillingReturn;
import com.erp.api.gresmanagement.model.PaginatedResultGresFillingReturn;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.service.gres.GresFillingReturnService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GresFillingReturnController
    extends AbstractCrudControllerV2<
        Long, NewGresFillingReturn, GresFillingReturn, Void, PaginatedResultGresFillingReturn>
    implements GresFillingReturnGresManagementApi {

  public GresFillingReturnController(GresFillingReturnService gresFillingReturnService) {
    super(gresFillingReturnService, gresFillingReturnService);
  }

  @Override
  public ResponseEntity<GresFillingReturn> createGresFillingReturn(
      Long gresFillingId, NewGresFillingReturn newGresFillingReturn) {
    return crud().createOne(gresFillingId, newGresFillingReturn);
  }

  @Override
  public ResponseEntity<Void> deleteGresFillingReturn(Long gresFillingId, Long id) {
    return crud().delete(gresFillingId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultGresFillingReturn> getAllGresFillingReturns(
      Long gresFillingId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            gresFillingId,
            GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<GresFillingReturn> getGresFillingReturnById(Long gresFillingId, Long id) {
    return crud().getById(gresFillingId, id);
  }

  @Override
  public ResponseEntity<GresFillingReturn> updateGresFillingReturn(
      Long gresFillingId, Long id, NewGresFillingReturn newGresFillingReturn) {
    return crud().update(gresFillingId, id, newGresFillingReturn);
  }
}

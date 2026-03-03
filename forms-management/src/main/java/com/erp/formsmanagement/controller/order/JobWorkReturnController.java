package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.JobWorkReturnOrderManagementApi;
import com.erp.api.ordermanagement.model.JobWorkReturn;
import com.erp.api.ordermanagement.model.NewJobWorkReturn;
import com.erp.api.ordermanagement.model.PaginatedResultJobWorkReturn;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.service.order.JobWorkReturnService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobWorkReturnController
    extends AbstractCrudControllerV2<Long, NewJobWorkReturn, JobWorkReturn, Void, PaginatedResultJobWorkReturn>
    implements JobWorkReturnOrderManagementApi {

  public JobWorkReturnController(JobWorkReturnService s) {
    super(s, s);
  }

  @Override
  public ResponseEntity<JobWorkReturn> createJobWorkReturn(
      Long orderItemId, Long jobWorkId, NewJobWorkReturn newJobWorkReturn) {
    return crud().createOne(jobWorkId, newJobWorkReturn);
  }

  @Override
  public ResponseEntity<Void> deleteJobWorkReturn(Long orderItemId, Long jobWorkId, Long id) {
    return crud().delete(jobWorkId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultJobWorkReturn> getAllJobWorkReturns(
      Long orderItemId,
      Long jobWorkId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            jobWorkId,
            GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<JobWorkReturn> getJobWorkReturnById(
      Long orderItemId, Long jobWorkId, Long id) {
    return crud().getById(jobWorkId, id);
  }

  @Override
  public ResponseEntity<JobWorkReturn> updateJobWorkReturn(
      Long orderItemId, Long jobWorkId, Long id, NewJobWorkReturn newJobWorkReturn) {
    return crud().update(jobWorkId, id, newJobWorkReturn);
  }
}

package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.JobWorkOrderManagementApi;
import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.service.order.JobWorkService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobWorkController
    extends AbstractCrudControllerV2<Long, NewJobWork, JobWork, Void, PaginatedResultJobWork>
    implements JobWorkOrderManagementApi {

  private final JobWorkService jobWorkService;

  public JobWorkController(JobWorkService jobWorkService) {
    super(jobWorkService, jobWorkService);
    this.jobWorkService = jobWorkService;
  }

  @Override
  public ResponseEntity<JobWork> createJobWork(Long orderItemId, NewJobWork newJobWork) {
    return crud().createOne(orderItemId, newJobWork);
  }

  @Override
  public ResponseEntity<Void> deleteJobWork(Long orderItemId, Long id) {
    return crud().delete(orderItemId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultJobWork> getAllJobWorks(
      Long orderItemId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            orderItemId,
            GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<JobWork> getJobWorkById(Long orderItemId, Long id) {
    return crud().getById(orderItemId, id);
  }

  @Override
  public ResponseEntity<JobWork> updateJobWork(Long orderItemId, Long id, NewJobWork newJobWork) {
    return crud().update(orderItemId, id, newJobWork);
  }

  @Override
  public ResponseEntity<JobWork> updateJobWorkStatus(
      Long orderItemId, Long id, UpdateJobWorkStatus updateJobWorkStatus) {
    return ResponseEntity.ok(
        jobWorkService.updateStatus(orderItemId, id, updateJobWorkStatus));
  }

  @Override
  public ResponseEntity<JobWork> updateJobWorkType(
      Long orderItemId, Long id, UpdateJobWorkType updateJobWorkType) {
    return ResponseEntity.ok(
        jobWorkService.updateType(orderItemId, id, updateJobWorkType));
  }
}

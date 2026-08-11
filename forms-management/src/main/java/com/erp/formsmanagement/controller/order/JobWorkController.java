package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.JobWorkOrderManagementApi;
import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.formsmanagement.service.order.JobWorkService;
import com.erp.formsmanagement.service.order.JobWorkStats;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

  /** Manual job work — not tied to any order item. */
  @PostMapping("/api/v1/job-works/manual")
  public ResponseEntity<JobWork> createManualJobWork(@RequestBody NewJobWork newJobWork) {
    return ResponseEntity.status(HttpStatus.CREATED).body(jobWorkService.createManual(newJobWork));
  }

  /** Update a Manual job work — not tied to any order item. */
  @PutMapping("/api/v1/job-works/manual/{id}")
  public ResponseEntity<JobWork> updateManualJobWork(
      @PathVariable Long id, @RequestBody NewJobWork newJobWork) {
    return ResponseEntity.ok(jobWorkService.updateManual(id, newJobWork));
  }

  /**
   * Global job-work listing used by the operations dashboard and the plating
   * analytics pages. Unlike the OpenAPI-generated per-order-item endpoint,
   * this returns all job works across every order item, optionally filtered
   * by {@code jobWorkType} (OUTSIDE / INHOUSE / MANUAL).
   */
  @GetMapping("/api/v1/job-works")
  public ResponseEntity<PaginatedResultJobWork> getAllJobWorksGlobal(
      @RequestParam(required = false) String jobWorkType,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sortByFields,
      @RequestParam(required = false) String direction) {
    JobWorkType typeFilter = null;
    if (jobWorkType != null && !jobWorkType.isBlank()) {
      typeFilter = JobWorkType.valueOf(jobWorkType);
    }
    return ResponseEntity.ok(
        jobWorkService.getAllGlobal(
            typeFilter,
            GetAllQuery.of(
                Optional.empty(),
                Optional.ofNullable(search),
                Optional.ofNullable(page),
                Optional.ofNullable(size),
                Optional.ofNullable(sortByFields),
                Optional.ofNullable(direction))));
  }

  /**
   * Global stat-card counts (total / completed / pending) honouring the same {@code jobWorkType} +
   * {@code search} filters as the global listing, so the cards stay correct while the list is
   * paginated server-side.
   */
  @GetMapping("/api/v1/job-works/stats")
  public ResponseEntity<JobWorkStats> getJobWorkStats(
      @RequestParam(required = false) String jobWorkType,
      @RequestParam(required = false) String search) {
    JobWorkType typeFilter = null;
    if (jobWorkType != null && !jobWorkType.isBlank()) {
      typeFilter = JobWorkType.valueOf(jobWorkType);
    }
    return ResponseEntity.ok(jobWorkService.getGlobalStats(typeFilter, search));
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

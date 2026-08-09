package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.JobWorkReturn;
import com.erp.api.ordermanagement.model.NewJobWorkReturn;
import com.erp.api.ordermanagement.model.PaginatedResultJobWorkReturn;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkReturnRepository;
import com.erp.formsmanagement.mapper.order.JobWorkReturnMapper;
import com.erp.formsmanagement.service.order.JobWorkReturnService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobWorkReturnServiceImpl
    extends AbstractSpecificationServiceV2<JobWorkReturnEntity, NewJobWorkReturn, JobWorkReturn, Long>
    implements JobWorkReturnService {

  private final JobWorkReturnRepository jobWorkReturnRepository;
  private final JobWorkRepository jobWorkRepository;

  public JobWorkReturnServiceImpl(
      JobWorkReturnRepository jobWorkReturnRepository,
      JobWorkRepository jobWorkRepository,
      JobWorkReturnMapper mapper) {
    super(jobWorkReturnRepository, mapper);
    this.jobWorkReturnRepository = jobWorkReturnRepository;
    this.jobWorkRepository = jobWorkRepository;
  }

  @Override
  protected void afterCreate(JobWorkReturnEntity entity, Long jobWorkId, NewJobWorkReturn request) {
    JobWorkEntity jobWork = resolveJobWork(jobWorkId);
    computeReturnKg(entity);
    validateReturnQuantity(jobWork, entity, 0L);
    entity.setJobWork(jobWork);
  }

  @Override
  protected void afterUpdate(JobWorkReturnEntity entity, Long jobWorkId, NewJobWorkReturn request) {
    JobWorkEntity jobWork = resolveJobWork(jobWorkId);
    computeReturnKg(entity);
    validateReturnQuantity(jobWork, entity, entity.getId());
    entity.setJobWork(jobWork);
  }

  /** Net Kg = grossKg (weighed once) - returnElementCount * petiWeightKg, matching the excel. */
  private void computeReturnKg(JobWorkReturnEntity entity) {
    double grossKg = entity.getGrossKg() != null ? entity.getGrossKg() : 0.0;
    double count = entity.getReturnElementCount() != null ? entity.getReturnElementCount() : 0.0;
    double petiWeightKg = entity.getPetiWeightKg() != null ? entity.getPetiWeightKg() : 0.0;
    entity.setReturnKg(Math.max(0.0, grossKg - (count * petiWeightKg)));
  }

  private void validateReturnQuantity(JobWorkEntity jobWork, JobWorkReturnEntity entity, Long excludeId) {
    if (entity.getReturnKg() != null && jobWork.getQtyKg() != null) {
      double alreadyReturned = jobWorkReturnRepository.sumReturnKgByJobWorkId(jobWork.getId(), excludeId);
      double newReturnKg = entity.getReturnKg() + (entity.getGhati() != null ? entity.getGhati() : 0.0);
      double total = alreadyReturned + newReturnKg;
      // Round both sides to 3 decimals (matching the frontend) before comparing so IEEE-754
      // epsilon doesn't reject a return that exactly fills the remaining quantity — e.g.
      // 201.9 + 1.3 == 203.20000000000002, which would falsely "exceed" a 203.2 kg job.
      if (round3(total) > round3(jobWork.getQtyKg())) {
        throw new IllegalArgumentException(
            String.format("Total returned quantity %.2f kg exceeds sent quantity %.2f kg", total, jobWork.getQtyKg()));
      }
    }
  }

  /** Rounds to 3 decimals so weight comparisons ignore floating-point noise, matching the UI. */
  private static double round3(double v) {
    return Math.round(v * 1000.0) / 1000.0;
  }

  private JobWorkEntity resolveJobWork(Long jobWorkId) {
    return jobWorkRepository
        .findById(jobWorkId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format(Constant.ENTITY_NOT_FOUND, jobWorkId)));
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultJobWorkReturn getAll(Long jobWorkId, GetAllQuery<Void> query) {
    Specification<JobWorkReturnEntity> spec =
        Specification.where(jobWorkReturnRepository.filterByJobWorkId(jobWorkId))
            .and(jobWorkReturnRepository.filterBySearch(query.search()));

    Page<JobWorkReturnEntity> results =
        jobWorkReturnRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultJobWorkReturn::new,
        PaginatedResultJobWorkReturn::setData);
  }
}

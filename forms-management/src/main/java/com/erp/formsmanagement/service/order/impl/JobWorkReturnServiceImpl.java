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
    validateReturnQuantity(jobWork, 0L, request);
    entity.setJobWork(jobWork);
  }

  @Override
  protected void afterUpdate(JobWorkReturnEntity entity, Long jobWorkId, NewJobWorkReturn request) {
    JobWorkEntity jobWork = resolveJobWork(jobWorkId);
    validateReturnQuantity(jobWork, entity.getId(), request);
    entity.setJobWork(jobWork);
  }

  private void validateReturnQuantity(JobWorkEntity jobWork, Long excludeId, NewJobWorkReturn request) {
    if (request.getReturnKg() != null && jobWork.getQtyKg() != null) {
      double alreadyReturned = jobWorkReturnRepository.sumReturnKgByJobWorkId(jobWork.getId(), excludeId);
      double newReturnKg = request.getReturnKg() + (request.getGhati() != null ? request.getGhati() : 0.0);
      double total = alreadyReturned + newReturnKg;
      if (total > jobWork.getQtyKg()) {
        throw new IllegalArgumentException(
            String.format("Total returned quantity %.2f kg exceeds sent quantity %.2f kg", total, jobWork.getQtyKg()));
      }
    }
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

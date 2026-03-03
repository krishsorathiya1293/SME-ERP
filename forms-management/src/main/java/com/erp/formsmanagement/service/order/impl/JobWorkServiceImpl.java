package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkStatus;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemRepository;
import com.erp.formsmanagement.mapper.order.JobWorkMapper;
import com.erp.formsmanagement.service.order.JobWorkService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobWorkServiceImpl
    extends AbstractSpecificationServiceV2<JobWorkEntity, NewJobWork, JobWork, Long>
    implements JobWorkService {

  private final JobWorkRepository jobWorkRepository;
  private final OrderItemRepository orderItemRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public JobWorkServiceImpl(
      JobWorkRepository jobWorkRepository,
      OrderItemRepository orderItemRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      JobWorkMapper jobWorkMapper) {
    super(jobWorkRepository, jobWorkMapper);
    this.jobWorkRepository = jobWorkRepository;
    this.orderItemRepository = orderItemRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    linkRelations(entity, orderItemId, request);
    assignJobWorkNo(entity);
  }

  @Override
  protected void afterUpdate(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    linkRelations(entity, orderItemId, request);
    // jobWorkNo is never changed on update — keep the originally assigned number
  }

  /**
   * Generates a sequential job work number scoped to the current calendar month. Reads the current
   * MAX for this month, increments by 1, so the sequence resets automatically when a new month
   * begins.
   */
  private void assignJobWorkNo(JobWorkEntity entity) {

    LocalDateTime now = LocalDateTime.now();

    YearMonth yearMonth = YearMonth.from(now);

    LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    Double max = orderItemRepository.findMaxJobWorkNoForMonth(startDate, endDate);

    double next = (max == null ? 0.0 : max) + 1.0;

    entity.getOrderItem().setJobWorkNo(next);
  }

  private void linkRelations(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    OrderItemEntity orderItem =
        orderItemRepository
            .findById(orderItemId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, orderItemId)));
    entity.setOrderItem(orderItem);

    PartyEntity party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    ItemBlueprintDataEntity size =
        itemBlueprintDataRepository
            .findById(request.getSizeId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getSizeId())));
    entity.setSize(size);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultJobWork getAll(Long orderItemId, GetAllQuery<Void> query) {
    Specification<JobWorkEntity> spec =
        Specification.where(jobWorkRepository.filterByOrderItemId(orderItemId))
            .and(jobWorkRepository.filterBySearch(query.search()));

    Page<JobWorkEntity> results =
        jobWorkRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results, mapper()::toDomain, PaginatedResultJobWork::new, PaginatedResultJobWork::setData);
  }

  @Override
  @Transactional
  public JobWork updateStatus(Long orderItemId, Long id, UpdateJobWorkStatus request) {
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    entity.setStatus(JobWorkStatus.valueOf(request.getStatus().name()));
    return mapper().toDomain(entity);
  }

  @Override
  @Transactional
  public JobWork updateType(Long orderItemId, Long id, UpdateJobWorkType request) {
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    entity.setJobWorkType(JobWorkType.valueOf(request.getJobWorkType().name()));
    return mapper().toDomain(entity);
  }
}

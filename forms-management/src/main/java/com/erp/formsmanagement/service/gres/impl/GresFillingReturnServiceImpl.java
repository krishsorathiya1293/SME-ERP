package com.erp.formsmanagement.service.gres.impl;

import com.erp.api.gresmanagement.model.GresFillingReturn;
import com.erp.api.gresmanagement.model.NewGresFillingReturn;
import com.erp.api.gresmanagement.model.PaginatedResultGresFillingReturn;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.gres.GresElementType;
import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
import com.erp.formsmanagement.domain.repository.gres.GresFillingReturnRepository;
import com.erp.formsmanagement.mapper.gres.GresFillingReturnMapper;
import com.erp.formsmanagement.service.gres.GresFillingReturnService;
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
public class GresFillingReturnServiceImpl
    extends AbstractSpecificationServiceV2<
        GresFillingReturnEntity, NewGresFillingReturn, GresFillingReturn, Long>
    implements GresFillingReturnService {

  private final GresFillingReturnRepository gresFillingReturnRepository;
  private final GresFillingRepository gresFillingRepository;

  public GresFillingReturnServiceImpl(
      GresFillingReturnRepository gresFillingReturnRepository,
      GresFillingRepository gresFillingRepository,
      GresFillingReturnMapper mapper) {
    super(gresFillingReturnRepository, mapper);
    this.gresFillingReturnRepository = gresFillingReturnRepository;
    this.gresFillingRepository = gresFillingRepository;
  }

  @Override
  protected void afterCreate(
      GresFillingReturnEntity entity, Long gresFillingId, NewGresFillingReturn request) {
    GresFillingEntity gresFilling = resolveGresFilling(gresFillingId);
    entity.setGresFilling(gresFilling);
    if (request.getElementType() != null) {
      entity.setElementType(GresElementType.valueOf(request.getElementType().name()));
    }
  }

  @Override
  protected void afterUpdate(
      GresFillingReturnEntity entity, Long gresFillingId, NewGresFillingReturn request) {
    GresFillingEntity gresFilling = resolveGresFilling(gresFillingId);
    entity.setGresFilling(gresFilling);
    if (request.getElementType() != null) {
      entity.setElementType(GresElementType.valueOf(request.getElementType().name()));
    }
  }

  private GresFillingEntity resolveGresFilling(Long gresFillingId) {
    return gresFillingRepository
        .findById(gresFillingId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format(Constant.ENTITY_NOT_FOUND, gresFillingId)));
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultGresFillingReturn getAll(Long gresFillingId, GetAllQuery<Void> query) {
    Specification<GresFillingReturnEntity> spec =
        Specification.where(gresFillingReturnRepository.filterByGresFillingId(gresFillingId))
            .and(gresFillingReturnRepository.filterBySearch(query.search()));

    Page<GresFillingReturnEntity> results =
        gresFillingReturnRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultGresFillingReturn::new,
        PaginatedResultGresFillingReturn::setData);
  }
}

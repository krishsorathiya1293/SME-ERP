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
    recompute(entity, gresFilling);
  }

  @Override
  protected void afterUpdate(
      GresFillingReturnEntity entity, Long gresFillingId, NewGresFillingReturn request) {
    GresFillingEntity gresFilling = resolveGresFilling(gresFillingId);
    entity.setGresFilling(gresFilling);
    if (request.getElementType() != null) {
      entity.setElementType(GresElementType.valueOf(request.getElementType().name()));
    }
    recompute(entity, gresFilling);
  }

  private static Double round3(Double value) {
    if (value == null) return null;
    return Math.round(value * 1000.0) / 1000.0;
  }

  /**
   * Excel formula: Net Kg = Kgs − Peti × 1-Peti Weight;
   * Ghati = Return Net − Job Net (positive = returned more than sent, negative = shortfall).
   * The forward job's Net is taken from its first item (Gres records are single-item).
   */
  private void recompute(GresFillingReturnEntity entity, GresFillingEntity gresFilling) {
    Double gross = entity.getGrossKg();
    Double count = entity.getReturnElementCount();
    Double tare = entity.getPetiWeightKg();
    if (gross != null && count != null && tare != null) {
      entity.setReturnKg(round3(Math.max(0.0, gross - count * tare)));
    } else if (gross != null && entity.getReturnKg() == null) {
      entity.setReturnKg(round3(gross));
    }

    Double jobNet =
        gresFilling.getItems().stream()
            .findFirst()
            .map(item -> item.getNetWeight())
            .orElse(null);
    Double returnNet = entity.getReturnKg();
    if (jobNet != null && returnNet != null) {
      entity.setGhati(round3(returnNet - jobNet));
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

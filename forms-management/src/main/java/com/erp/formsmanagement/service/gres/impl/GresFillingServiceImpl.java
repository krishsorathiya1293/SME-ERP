package com.erp.formsmanagement.service.gres.impl;

import com.erp.api.gresmanagement.model.GresFilling;
import com.erp.api.gresmanagement.model.GresFillingItem;
import com.erp.api.gresmanagement.model.NewGresFilling;
import com.erp.api.gresmanagement.model.NewGresFillingItem;
import com.erp.api.gresmanagement.model.PaginatedResultGresFilling;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.gres.GresElementType;
import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.gres.GresFillingItemEntity;
import com.erp.formsmanagement.domain.entity.gres.GresFillingStatus;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
import com.erp.formsmanagement.mapper.gres.GresFillingItemMapper;
import com.erp.formsmanagement.mapper.gres.GresFillingMapper;
import com.erp.formsmanagement.service.gres.GresFillingService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GresFillingServiceImpl
    extends AbstractSpecificationServiceV1<GresFillingEntity, NewGresFilling, GresFilling>
    implements GresFillingService {

  private final GresFillingRepository gresFillingRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final GresFillingItemMapper gresFillingItemMapper;

  public GresFillingServiceImpl(
      GresFillingRepository gresFillingRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      GresFillingMapper gresFillingMapper,
      GresFillingItemMapper gresFillingItemMapper) {
    super(gresFillingRepository, gresFillingMapper);
    this.gresFillingRepository = gresFillingRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.gresFillingItemMapper = gresFillingItemMapper;
  }

  @Override
  protected void afterCreate(GresFillingEntity entity, NewGresFilling request) {
    linkRelations(entity, request);
    assignChitthiNo(entity);
    if (entity.getStatus() == null) {
      entity.setStatus(GresFillingStatus.PENDING);
    }
  }

  @Override
  protected void afterUpdate(GresFillingEntity entity, NewGresFilling request) {
    linkRelations(entity, request);
  }

  private void assignChitthiNo(GresFillingEntity entity) {
    long count = gresFillingRepository.count() + 1;
    int year = Year.now().getValue();
    entity.setChitthiNo(String.format("%02d/Gres.Fill/%d", count, year));
  }

  private void linkRelations(GresFillingEntity entity, NewGresFilling request) {
    PartyEntity party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Sync items
    entity.getItems().clear();
    if (request.getItems() != null) {
      List<GresFillingItemEntity> itemEntities = new ArrayList<>();
      for (NewGresFillingItem newItem : request.getItems()) {
        GresFillingItemEntity itemEntity = gresFillingItemMapper.toEntity(newItem);
        itemEntity.setGresFilling(entity);
        if (newItem.getSizeId() != null) {
          ItemBlueprintDataEntity size =
              itemBlueprintDataRepository
                  .findById(newItem.getSizeId())
                  .orElseThrow(
                      () ->
                          new EntityNotFoundException(
                              String.format(Constant.ENTITY_NOT_FOUND, newItem.getSizeId())));
          itemEntity.setSize(size);
        }
        if (newItem.getElementType() != null) {
          itemEntity.setElementType(GresElementType.valueOf(newItem.getElementType().name()));
        }
        itemEntities.add(itemEntity);
      }
      entity.getItems().addAll(itemEntities);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultGresFilling getAll(GetAllQuery<Void> query) {
    Specification<GresFillingEntity> spec =
        Specification.where(gresFillingRepository.filterBySearch(query.search()));

    Page<GresFillingEntity> results =
        gresFillingRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultGresFilling::new,
        PaginatedResultGresFilling::setData);
  }
}

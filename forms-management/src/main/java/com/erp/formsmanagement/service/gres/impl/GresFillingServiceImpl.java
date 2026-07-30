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
import java.time.LocalDate;
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

  /**
   * Assigns a monthly-reset Ch. No. per the client's Excel spec ("001" that starts at 1 each
   * calendar month). We keep the pair (year-month, serial) unique in the DB, and set the display
   * chitthiNo to the zero-padded serial for convenience.
   */
  private void assignChitthiNo(GresFillingEntity entity) {
    LocalDate date = entity.getChitthiDate() != null ? entity.getChitthiDate() : LocalDate.now();
    String yearMonth = String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    Integer max = gresFillingRepository.findMaxChNoSerialForYearMonth(yearMonth);
    int next = (max == null ? 0 : max) + 1;
    entity.setChNoYearMonth(yearMonth);
    entity.setChNoSerial(next);
    entity.setChitthiNo(String.format("%03d", next));
  }

  private static Double round3(Double value) {
    if (value == null) return null;
    return Math.round(value * 1000.0) / 1000.0;
  }

  /**
   * Excel formula: Net Kg = Gross Kg − Peti count × 1-Peti tare weight;
   * Total Rate = Net Kg × Rate/Kg (rounded to integer per the printout).
   * Runs on every save so the numbers can't drift from the inputs.
   */
  private void computeItemTotals(GresFillingItemEntity item) {
    Double gross = item.getUnitKg();
    Double count = item.getElementCount();
    Double tare = item.getPetiWeightKg();
    if (gross != null && count != null && tare != null) {
      double net = Math.max(0.0, gross - count * tare);
      item.setNetWeight(round3(net));
    } else if (gross != null && (count == null || tare == null)) {
      // If we only have gross, fall back to it as net (user hasn't entered Peti yet).
      item.setNetWeight(round3(gross));
    }

    Double net = item.getNetWeight();
    Double rate = item.getRatePerKg();
    if (net != null && rate != null) {
      item.setTotalAmount((double) Math.round(net * rate));
    }
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

    // Sync items only when the caller sent an items list. A missing list means
    // "leave existing items alone" (e.g. status-only updates from the card).
    if (request.getItems() != null) {
      entity.getItems().clear();
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
        computeItemTotals(itemEntity);
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

package com.erp.formsmanagement.service.packinginvoice.impl;

import com.erp.api.packinginvoicemanagement.model.NewPackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PaginatedResultPackingInvoice;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceEntity;
import com.erp.formsmanagement.domain.entity.packinginvoice.filter.PackingInvoiceFilter;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.packinginvoice.PackingInvoiceRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.packinginvoice.PackingInvoiceMapper;
import com.erp.formsmanagement.service.packinginvoice.PackingInvoiceService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PackingInvoiceServiceImpl
    extends AbstractSpecificationServiceV1<PackingInvoiceEntity, NewPackingInvoice, PackingInvoice>
    implements PackingInvoiceService {

  private final PackingInvoiceRepository packingInvoiceRepository;
  private final PackingInvoiceMapper packingInvoiceMapper;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public PackingInvoiceServiceImpl(
      PackingInvoiceRepository packingInvoiceRepository,
      PackingInvoiceMapper packingInvoiceMapper,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository) {
    super(packingInvoiceRepository, packingInvoiceMapper);
    this.packingInvoiceRepository = packingInvoiceRepository;
    this.packingInvoiceMapper = packingInvoiceMapper;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(PackingInvoiceEntity entity, NewPackingInvoice request) {
    // Set party
    var party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Determine invoice date
    LocalDate invoiceDate =
        request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now();
    entity.setInvoiceDate(invoiceDate);

    // Auto-generate invoiceNo: sequential per party+date
    long existingCount =
        packingInvoiceRepository.countByInvoiceDateAndParty_Id(invoiceDate, party.getId());
    entity.setInvoiceNo(String.format("%02d", existingCount + 1));

    // Auto-generate cartoonNo: cumulative per party
    long totalPartyInvoices = packingInvoiceRepository.countByParty_Id(party.getId());
    entity.setCartoonNo(String.format("%02d", totalPartyInvoices + 1));

    // Resolve size entities for items
    resolveItemSizes(entity, request);
  }

  @Override
  protected void afterUpdate(PackingInvoiceEntity entity, NewPackingInvoice request) {
    // Update party if changed
    var party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Update invoice date if provided
    if (request.getInvoiceDate() != null) {
      entity.setInvoiceDate(request.getInvoiceDate());
    }

    // Resolve size entities for items
    resolveItemSizes(entity, request);
  }

  private void resolveItemSizes(PackingInvoiceEntity entity, NewPackingInvoice request) {
    if (entity.getItems() == null || request.getItems() == null) return;
    var items = entity.getItems();
    var requestItems = request.getItems();
    int count = Math.min(items.size(), requestItems.size());
    for (int i = 0; i < count; i++) {
      Long sizeId = requestItems.get(i).getSizeId();
      if (sizeId != null) {
        ItemBlueprintDataEntity size =
            itemBlueprintDataRepository
                .findById(sizeId)
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            String.format(Constant.ENTITY_NOT_FOUND, sizeId)));
        items.get(i).setSize(size);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultPackingInvoice getAll(GetAllQuery<PackingInvoiceFilter> query) {
    Specification<PackingInvoiceEntity> spec = buildSpec(query);
    Page<PackingInvoiceEntity> page =
        packingInvoiceRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        page,
        packingInvoiceMapper::toDomain,
        PaginatedResultPackingInvoice::new,
        PaginatedResultPackingInvoice::setData);
  }

  private Specification<PackingInvoiceEntity> buildSpec(GetAllQuery<PackingInvoiceFilter> query) {
    Specification<PackingInvoiceEntity> spec = Specification.where(null);

    if (query.filter().isPresent()) {
      PackingInvoiceFilter filter = query.filter().get();

      if (filter.partyId() != null) {
        spec = spec.and((root, q, cb) -> cb.equal(root.get("party").get("id"), filter.partyId()));
      }

      if (filter.month() != null) {
        LocalDate start = filter.month().atDay(1);
        LocalDate end = filter.month().atEndOfMonth();
        spec = spec.and((root, q, cb) -> cb.between(root.get("invoiceDate"), start, end));
      }
    }

    return spec;
  }
}

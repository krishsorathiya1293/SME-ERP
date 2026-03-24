package com.erp.formsmanagement.service.purchase.impl;

import com.erp.api.purchasemanagement.model.NewPurchaseOrder;
import com.erp.api.purchasemanagement.model.PaginatedResultPurchaseOrder;
import com.erp.api.purchasemanagement.model.PurchaseOrder;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.purchase.PurchaseOrderRepository;
import com.erp.formsmanagement.mapper.purchase.PurchaseOrderMapper;
import com.erp.formsmanagement.service.purchase.PurchaseOrderService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PurchaseOrderServiceImpl
    extends AbstractSpecificationServiceV1<PurchaseOrderEntity, NewPurchaseOrder, PurchaseOrder>
    implements PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final PurchaseOrderMapper purchaseOrderMapper;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public PurchaseOrderServiceImpl(
      PurchaseOrderRepository purchaseOrderRepository,
      PurchaseOrderMapper purchaseOrderMapper,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository) {
    super(purchaseOrderRepository, purchaseOrderMapper);
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.purchaseOrderMapper = purchaseOrderMapper;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(PurchaseOrderEntity entity, NewPurchaseOrder request) {
    // Set party (seller)
    var party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Auto-generate purchaseNo: sequential globally per year
    long count = purchaseOrderRepository.count();
    String year = String.valueOf(java.time.LocalDate.now().getYear());
    entity.setPurchaseNo(String.format("%d/PUR-%02d-%s", count + 1, count + 1, year));

    // Resolve size entities for items
    resolveItemSizes(entity, request);
  }

  @Override
  protected void afterUpdate(PurchaseOrderEntity entity, NewPurchaseOrder request) {
    var party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Resolve size entities for items
    resolveItemSizes(entity, request);
  }

  private void resolveItemSizes(PurchaseOrderEntity entity, NewPurchaseOrder request) {
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
  public PaginatedResultPurchaseOrder getAll(GetAllQuery<String> query) {
    Specification<PurchaseOrderEntity> spec = Specification.where(null);

    if (query.search().isPresent()) {
      String search = "%" + query.search().get().toLowerCase() + "%";
      spec =
          spec.and(
              (root, q, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("purchaseNo")), search),
                      cb.like(cb.lower(root.join("party").get("name")), search)));
    }

    Page<PurchaseOrderEntity> page =
        purchaseOrderRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        page,
        purchaseOrderMapper::toDomain,
        PaginatedResultPurchaseOrder::new,
        PaginatedResultPurchaseOrder::setData);
  }
}

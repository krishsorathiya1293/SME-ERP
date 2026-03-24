package com.erp.formsmanagement.service.sales.impl;

import com.erp.api.salesmanagement.model.NewSalesOrder;
import com.erp.api.salesmanagement.model.PaginatedResultSalesOrder;
import com.erp.api.salesmanagement.model.SalesOrder;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.sales.SalesOrderRepository;
import com.erp.formsmanagement.mapper.sales.SalesOrderMapper;
import com.erp.formsmanagement.service.sales.SalesOrderService;
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
public class SalesOrderServiceImpl
    extends AbstractSpecificationServiceV1<SalesOrderEntity, NewSalesOrder, SalesOrder>
    implements SalesOrderService {

  private final SalesOrderRepository salesOrderRepository;
  private final SalesOrderMapper salesOrderMapper;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public SalesOrderServiceImpl(
      SalesOrderRepository salesOrderRepository,
      SalesOrderMapper salesOrderMapper,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository) {
    super(salesOrderRepository, salesOrderMapper);
    this.salesOrderRepository = salesOrderRepository;
    this.salesOrderMapper = salesOrderMapper;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(SalesOrderEntity entity, NewSalesOrder request) {
    // Set party (customer)
    var party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    // Auto-generate salesNo: sequential globally per year
    long count = salesOrderRepository.count();
    String year = String.valueOf(java.time.LocalDate.now().getYear());
    entity.setSalesNo(String.format("%d/Sales-%02d-%s", count + 1, count + 1, year));

    // Resolve size entities for items
    resolveItemSizes(entity, request);
  }

  @Override
  protected void afterUpdate(SalesOrderEntity entity, NewSalesOrder request) {
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

  private void resolveItemSizes(SalesOrderEntity entity, NewSalesOrder request) {
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
  public PaginatedResultSalesOrder getAll(GetAllQuery<String> query) {
    Specification<SalesOrderEntity> spec = Specification.where(null);

    if (query.search().isPresent()) {
      String search = "%" + query.search().get().toLowerCase() + "%";
      spec =
          spec.and(
              (root, q, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("salesNo")), search),
                      cb.like(cb.lower(root.join("party").get("name")), search)));
    }

    Page<SalesOrderEntity> page =
        salesOrderRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        page,
        salesOrderMapper::toDomain,
        PaginatedResultSalesOrder::new,
        PaginatedResultSalesOrder::setData);
  }
}

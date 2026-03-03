package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.ItemRepository;
import com.erp.formsmanagement.mapper.master.ItemMapper;
import com.erp.formsmanagement.service.master.ItemService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ItemServiceImpl
    extends AbstractSpecificationServiceV1<ItemEntity, NewItem, Item>
    implements ItemService {

  private final ItemRepository itemRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public ItemServiceImpl(
      ItemRepository itemRepository,
      ItemMapper itemMapper,
      ItemBlueprintDataRepository itemBlueprintDataRepository) {
    super(itemRepository, itemMapper);
    this.itemRepository = itemRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(ItemEntity entity, NewItem request) {
    entity.setSize(
        itemBlueprintDataRepository
            .findById(request.getSizeId())
            .orElseThrow(
                () -> new EntityNotFoundException("Size not found with id: " + request.getSizeId())));
  }

  @Override
  protected void afterUpdate(ItemEntity entity, NewItem request) {
    afterCreate(entity, request);
  }

  @Override
  public PaginatedResultItem getAll(GetAllQuery<String> query) {
    Specification<ItemEntity> spec =
        Specification.where(itemRepository.filterByStatus(query.filter()))
            .and(itemRepository.filterBySearch(query.search()));
    Page<ItemEntity> results =
        itemRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        results, mapper()::toDomain, PaginatedResultItem::new, PaginatedResultItem::setData);
  }
}

package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.Inventory;
import com.erp.api.itemmanagement.model.NewInventory;
import com.erp.api.itemmanagement.model.PaginatedResultInventory;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.entity.inventory.filter.InventoryFilter;
import com.erp.formsmanagement.domain.repository.inventory.InventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.mapper.inventory.InventoryMapper;
import com.erp.formsmanagement.service.inventory.InventoryService;
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
public class InventoryServiceImpl
    extends AbstractSpecificationServiceV2<InventoryEntity, NewInventory, Inventory, Long>
    implements InventoryService {

  private final InventoryRepository inventoryRepository;
  private final ItemBlueprintRepository itemBlueprintRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public InventoryServiceImpl(
      InventoryRepository inventoryRepository,
      ItemBlueprintRepository itemBlueprintRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      InventoryMapper inventoryMapper) {
    super(inventoryRepository, inventoryMapper);
    this.inventoryRepository = inventoryRepository;
    this.itemBlueprintRepository = itemBlueprintRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(InventoryEntity entity, Long parentId, NewInventory request) {
    linkBlueprintData(parentId, entity, request);
  }

  @Override
  protected void afterUpdate(InventoryEntity entity, Long parentId, NewInventory request) {
    linkBlueprintData(parentId, entity, request);
  }

  private void linkBlueprintData(Long itemId, InventoryEntity entity, NewInventory request) {
    ItemBlueprintEntity item =
        itemBlueprintRepository
            .findById(itemId)
            .orElseThrow(
                () -> new EntityNotFoundException("Item Blueprint not found with id: " + itemId));

    ItemBlueprintDataEntity size =
        itemBlueprintDataRepository
            .findByItem_IdAndSizeInInchAndSizeInMm(
                item.getId(), request.getSizeInInch(), request.getSizeInMm())
            .orElseGet(
                () -> {
                  ItemBlueprintDataEntity newSize = new ItemBlueprintDataEntity();
                  newSize.setItem(item);
                  newSize.setSizeInInch(request.getSizeInInch());
                  newSize.setSizeInMm(request.getSizeInMm());
                  if (request.getDozenWeight() != null) {
                    newSize.setDozenWeight(request.getDozenWeight());
                  }
                  return itemBlueprintDataRepository.save(newSize);
                });

    entity.setSize(size);
  }

  @Override
  @Transactional(readOnly = true)
  public Inventory getById(Long itemId, Long id) {
    InventoryEntity entity =
        inventoryRepository
            .findById(id)
            .filter(inv -> inv.getSize().getItem().getId().equals(itemId))
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Inventory not found with id: " + id + " for item id: " + itemId));
    return mapper().toDomain(entity);
  }

  @Override
  public void deleteById(Long itemId, Long id) {
    InventoryEntity entity =
        inventoryRepository
            .findById(id)
            .filter(inv -> inv.getSize().getItem().getId().equals(itemId))
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Inventory not found with id: " + id + " for item id: " + itemId));
    ItemBlueprintDataEntity size = entity.getSize();
    size.setInventory(null);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultInventory getAll(Long itemId, GetAllQuery<InventoryFilter> query) {
    Specification<InventoryEntity> spec =
        Specification.where(inventoryRepository.filterByItemId(itemId))
            .and(inventoryRepository.filterBySearch(query.search()))
            .and(inventoryRepository.filterByInventoryFilter(query.filter()));

    Page<InventoryEntity> results =
        inventoryRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultInventory::new,
        PaginatedResultInventory::setData);
  }
}

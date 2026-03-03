package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.mapper.inventory.ItemBlueprintDataMapper;
import com.erp.formsmanagement.service.inventory.ItemBlueprintDataService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemBlueprintDataServiceImpl
    extends AbstractSpecificationServiceV2<ItemBlueprintDataEntity, NewSize, Size, Long>
    implements ItemBlueprintDataService {

  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ItemBlueprintRepository itemBlueprintRepository;

  public ItemBlueprintDataServiceImpl(
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      ItemBlueprintRepository itemBlueprintRepository,
      ItemBlueprintDataMapper itemBlueprintDataMapper) {
    super(itemBlueprintDataRepository, itemBlueprintDataMapper);
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.itemBlueprintRepository = itemBlueprintRepository;
  }

  @Override
  protected void afterCreate(ItemBlueprintDataEntity entity, Long itemId, NewSize request) {
    ItemBlueprintEntity item =
        itemBlueprintRepository
            .findById(itemId)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, itemId)));
    entity.setItem(item);
  }

  @Override
  protected void afterUpdate(ItemBlueprintDataEntity entity, Long itemId, NewSize request) {
    afterCreate(entity, itemId, request);
  }

  /** Override to add parent-ownership check before returning. */
  @Override
  public Size getById(Long itemId, Long sizeId) {
    return itemBlueprintDataRepository
        .findById(sizeId)
        .filter(e -> e.getItem().getId().equals(itemId))
        .map(e -> mapper().toDomain(e))
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Size not found with id: " + sizeId + " for item: " + itemId));
  }

  /** Override to add parent-ownership check before deleting. */
  @Override
  @Transactional
  public void deleteById(Long itemId, Long sizeId) {
    ItemBlueprintDataEntity entity =
        itemBlueprintDataRepository
            .findById(sizeId)
            .filter(e -> e.getItem().getId().equals(itemId))
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Size not found with id: " + sizeId + " for item: " + itemId));
    itemBlueprintDataRepository.delete(entity);
  }

  @Override
  public List<Size> getAll(Long itemId, GetAllQuery<Void> query) {
    Specification<ItemBlueprintDataEntity> spec =
        (root, q, cb) -> {
          var itemPredicate = cb.equal(root.get("item").get("id"), itemId);
          String search = query.search().orElse(null);
          if (search == null || search.isBlank()) {
            return itemPredicate;
          }
          String pattern = "%" + search.toLowerCase() + "%";
          var searchPredicate =
              cb.or(
                  cb.like(cb.lower(root.get("sizeInInch")), pattern),
                  cb.like(cb.lower(root.get("sizeInMm")), pattern));
          return cb.and(itemPredicate, searchPredicate);
        };
    return itemBlueprintDataRepository.findAll(spec).stream().map(e -> mapper().toDomain(e)).toList();
  }
}

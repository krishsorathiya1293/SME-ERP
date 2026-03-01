package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.Size;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.mapper.inventory.ItemBlueprintDataMapper;
import com.erp.formsmanagement.service.inventory.ItemBlueprintDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemBlueprintDataServiceImpl implements ItemBlueprintDataService {

  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ItemBlueprintDataMapper itemBlueprintDataMapper;
  private final com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository itemBlueprintRepository;

  @Override
  public List<Size> getSizesByItemId(Long itemId, String search) {
    Specification<ItemBlueprintDataEntity> spec =
        (root, query, cb) -> {
          var itemPredicate = cb.equal(root.get("item").get("id"), itemId);

          if (search == null || search.isBlank()) {
            return itemPredicate;
          }

          String searchPattern = "%" + search.toLowerCase() + "%";
          var searchPredicate =
              cb.or(
                  cb.like(cb.lower(root.get("sizeInInch")), searchPattern),
                  cb.like(cb.lower(root.get("sizeInMm")), searchPattern));

          return cb.and(itemPredicate, searchPredicate);
        };

    return itemBlueprintDataMapper.toDomainList(itemBlueprintDataRepository.findAll(spec));
  }

  @Override
  public Size getSizeById(Long itemId, Long sizeId) {
    return itemBlueprintDataRepository
        .findById(sizeId)
        .filter(size -> size.getItem().getId().equals(itemId))
        .map(itemBlueprintDataMapper::toDomain)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Size not found with id: " + sizeId + " for item: " + itemId));
  }
  @Override
  @Transactional
  public Size createSize(Long itemId, com.erp.api.itemmanagement.model.NewSize newSize) {
    com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity item = itemBlueprintRepository.findById(itemId)
        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemId));
        
    ItemBlueprintDataEntity entity = itemBlueprintDataMapper.toEntity(newSize);
    entity.setItem(item);
    return itemBlueprintDataMapper.toDomain(itemBlueprintDataRepository.save(entity));
  }

  @Override
  @Transactional
  public Size updateSize(Long itemId, Long sizeId, com.erp.api.itemmanagement.model.NewSize newSize) {
    ItemBlueprintDataEntity entity = itemBlueprintDataRepository.findById(sizeId)
        .filter(size -> size.getItem().getId().equals(itemId))
        .orElseThrow(() -> new EntityNotFoundException("Size not found with id: " + sizeId + " for item: " + itemId));
        
    itemBlueprintDataMapper.updateEntityFromDto(newSize, entity);
    return itemBlueprintDataMapper.toDomain(itemBlueprintDataRepository.save(entity));
  }

  @Override
  @Transactional
  public void deleteSize(Long itemId, Long sizeId) {
    ItemBlueprintDataEntity entity = itemBlueprintDataRepository.findById(sizeId)
        .filter(size -> size.getItem().getId().equals(itemId))
        .orElseThrow(() -> new EntityNotFoundException("Size not found with id: " + sizeId + " for item: " + itemId));
        
    itemBlueprintDataRepository.delete(entity);
  }
}

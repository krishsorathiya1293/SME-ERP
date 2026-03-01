package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.Item;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.mapper.inventory.ItemBlueprintMapper;
import com.erp.formsmanagement.service.inventory.ItemBlueprintService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemBlueprintServiceImpl implements ItemBlueprintService {

  private final ItemBlueprintRepository itemBlueprintRepository;
  private final ItemBlueprintMapper itemBlueprintMapper;

  @Override
  public List<Item> getAllItems(String search) {
    Specification<ItemBlueprintEntity> spec =
        (root, query, cb) -> {
          if (search == null || search.isBlank()) {
            return cb.conjunction();
          }
          return cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%");
        };
    return itemBlueprintMapper.toDomainList(itemBlueprintRepository.findAll(spec));
  }

  @Override
  public Item getItemById(Long id) {
    return itemBlueprintRepository
        .findById(id)
        .map(itemBlueprintMapper::toDomain)
        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
  }
  @Override
  @Transactional
  public Item createItem(com.erp.api.itemmanagement.model.NewItem newItem) {
    ItemBlueprintEntity entity = itemBlueprintMapper.toEntity(newItem);
    return itemBlueprintMapper.toDomain(itemBlueprintRepository.save(entity));
  }

  @Override
  @Transactional
  public Item updateItem(Long id, com.erp.api.itemmanagement.model.NewItem newItem) {
    ItemBlueprintEntity entity = itemBlueprintRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
    itemBlueprintMapper.updateEntityFromDto(newItem, entity);
    return itemBlueprintMapper.toDomain(itemBlueprintRepository.save(entity));
  }

  @Override
  @Transactional
  public void deleteItem(Long id) {
    ItemBlueprintEntity entity = itemBlueprintRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
    itemBlueprintRepository.delete(entity);
  }
}

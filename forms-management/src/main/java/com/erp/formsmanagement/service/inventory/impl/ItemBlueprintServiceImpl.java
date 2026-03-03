package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.mapper.inventory.ItemBlueprintMapper;
import com.erp.formsmanagement.service.inventory.ItemBlueprintService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemBlueprintServiceImpl
    extends AbstractSpecificationServiceV1<ItemBlueprintEntity, NewItem, Item>
    implements ItemBlueprintService {

  private final ItemBlueprintRepository itemBlueprintRepository;

  public ItemBlueprintServiceImpl(
      ItemBlueprintRepository itemBlueprintRepository, ItemBlueprintMapper itemBlueprintMapper) {
    super(itemBlueprintRepository, itemBlueprintMapper);
    this.itemBlueprintRepository = itemBlueprintRepository;
  }

  @Override
  public List<Item> getAll(GetAllQuery<Void> query) {
    Specification<ItemBlueprintEntity> spec =
        (root, q, cb) -> {
          String search = query.search().orElse(null);
          if (search == null || search.isBlank()) {
            return cb.conjunction();
          }
          return cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%");
        };
    return itemBlueprintRepository.findAll(spec).stream().map(e -> mapper().toDomain(e)).toList();
  }
}

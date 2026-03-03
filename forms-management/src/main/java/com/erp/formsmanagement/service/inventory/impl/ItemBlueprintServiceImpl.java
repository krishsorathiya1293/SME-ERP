package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.domain.repository.master.CategoryRepository;
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
  private final CategoryRepository categoryRepository;

  public ItemBlueprintServiceImpl(
      ItemBlueprintRepository itemBlueprintRepository,
      ItemBlueprintMapper itemBlueprintMapper,
      CategoryRepository categoryRepository) {
    super(itemBlueprintRepository, itemBlueprintMapper);
    this.itemBlueprintRepository = itemBlueprintRepository;
    this.categoryRepository = categoryRepository;
  }

  @Override
  protected void afterCreate(ItemBlueprintEntity entity, NewItem request) {
    entity.setCategory(
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(
                () -> new EntityNotFoundException(
                    String.format(Constant.ENTITY_NOT_FOUND, request.getCategoryId()))));
  }

  @Override
  protected void afterUpdate(ItemBlueprintEntity entity, NewItem request) {
    afterCreate(entity, request);
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

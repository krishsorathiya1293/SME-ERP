package com.erp.formsmanagement.domain.repository.inventory;

import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.repository.CoreRepository;

import java.util.Optional;

public interface ItemBlueprintRepository extends CoreRepository<ItemBlueprintEntity, Long> {
  Optional<ItemBlueprintEntity> findByItemName(String itemName);
}

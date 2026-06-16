package com.erp.formsmanagement.domain.repository.inventory;

import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.repository.CoreRepository;

import java.util.Optional;

public interface ItemBlueprintDataRepository extends CoreRepository<ItemBlueprintDataEntity, Long> {
  Optional<ItemBlueprintDataEntity> findByItem_IdAndSizeInInchAndSizeInMm(
      Long itemId, String sizeInInch, String sizeInMm);

  Optional<ItemBlueprintDataEntity> findFirstBySizeInInchAndSizeInMm(
      String sizeInInch, String sizeInMm);
}

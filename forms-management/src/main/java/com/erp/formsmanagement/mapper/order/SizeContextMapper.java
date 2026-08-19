package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.JobWorkSize;
import com.erp.api.ordermanagement.model.OrderItemSize;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;

/**
 * Shared enrichment for a Size (ItemBlueprintDataEntity) into the item-name/category/weight
 * and packaging-rate context that Job Work needs to auto-calculate pieces/stickers/cartons
 * without asking the user to re-enter master data that already exists on the item.
 */
public final class SizeContextMapper {

  private SizeContextMapper() {}

  public static JobWorkSize toJobWorkSize(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    JobWorkSize s = new JobWorkSize();
    s.setId(size.getId());
    s.setSizeInInch(size.getSizeInInch());
    s.setSizeInMm(size.getSizeInMm());
    s.setDozenWeight(size.getDozenWeight());
    s.setPcsWeight(size.getPcsWeight());
    applyItem(size, s::setItemName, s::setCategory);
    applyInventory(size, s::setPcsPerBox, s::setBoxPerCarton, s::setPcsPerCarton);
    return s;
  }

  public static OrderItemSize toOrderItemSize(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    OrderItemSize s = new OrderItemSize();
    s.setId(size.getId());
    s.setSizeInInch(size.getSizeInInch());
    s.setSizeInMm(size.getSizeInMm());
    s.setDozenWeight(size.getDozenWeight());
    s.setPcsWeight(size.getPcsWeight());
    applyItem(size, s::setItemName, s::setCategory);
    applyInventory(size, s::setPcsPerBox, s::setBoxPerCarton, s::setPcsPerCarton);
    return s;
  }

  private interface StringSetter {
    void set(String value);
  }

  private interface DoubleSetter {
    void set(Double value);
  }

  private static void applyItem(
      ItemBlueprintDataEntity size, StringSetter itemNameSetter, StringSetter categorySetter) {
    ItemBlueprintEntity item = size.getItem();
    if (item == null) return;
    itemNameSetter.set(item.getItemName());
    if (item.getCategory() != null) {
      categorySetter.set(item.getCategory().getName());
    }
  }

  private static void applyInventory(
      ItemBlueprintDataEntity size,
      DoubleSetter pcsPerBoxSetter,
      DoubleSetter boxPerCartonSetter,
      DoubleSetter pcsPerCartonSetter) {
    InventoryEntity inventory = size.getInventory();
    if (inventory == null) return;
    if (inventory.getPcsPerBox() != null) pcsPerBoxSetter.set(inventory.getPcsPerBox().doubleValue());
    if (inventory.getBoxPerCarton() != null) boxPerCartonSetter.set(inventory.getBoxPerCarton().doubleValue());
    if (inventory.getPcsPerCarton() != null) pcsPerCartonSetter.set(inventory.getPcsPerCarton().doubleValue());
  }
}

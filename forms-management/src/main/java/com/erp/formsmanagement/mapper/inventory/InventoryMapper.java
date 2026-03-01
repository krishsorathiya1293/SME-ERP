package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.Inventory;
import com.erp.api.itemmanagement.model.NewInventory;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InventoryMapper extends EntityMapper<InventoryEntity, NewInventory, Inventory> {

  @Mapping(source = "size.item.itemName", target = "itemName")
  @Mapping(source = "size.sizeInInch", target = "sizeInInch")
  @Mapping(source = "size.sizeInMm", target = "sizeInMm")
  @Mapping(source = "size.dozenWeight", target = "dozenWeight")
  Inventory toDomain(InventoryEntity entity);

  @Mapping(target = "size", ignore = true)
  InventoryEntity toEntity(NewInventory newInventory);

  @Mapping(target = "size", ignore = true)
  @Mapping(target = "id", ignore = true)
  void updateEntity(@MappingTarget InventoryEntity entity, NewInventory newInventory);
}

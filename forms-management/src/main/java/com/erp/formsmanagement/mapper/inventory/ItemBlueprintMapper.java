package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.Item;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ItemBlueprintDataMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemBlueprintMapper {
  Item toDomain(ItemBlueprintEntity entity);
  List<Item> toDomainList(List<ItemBlueprintEntity> entities);
  ItemBlueprintEntity toEntity(com.erp.api.itemmanagement.model.NewItem dto);
  void updateEntityFromDto(com.erp.api.itemmanagement.model.NewItem dto, @org.mapstruct.MappingTarget ItemBlueprintEntity entity);
}

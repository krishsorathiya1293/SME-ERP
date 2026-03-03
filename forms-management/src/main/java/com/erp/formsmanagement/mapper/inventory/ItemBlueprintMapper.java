package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.mapper.EntityMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {ItemBlueprintDataMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemBlueprintMapper extends EntityMapper<ItemBlueprintEntity, NewItem, Item> {

  @Override
  Item toDomain(ItemBlueprintEntity entity);

  @Override
  ItemBlueprintEntity toEntity(NewItem dto);

  @Override
  void updateEntity(@MappingTarget ItemBlueprintEntity entity, NewItem dto);

  List<Item> toDomainList(List<ItemBlueprintEntity> entities);
}

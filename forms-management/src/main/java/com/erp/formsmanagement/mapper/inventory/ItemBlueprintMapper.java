package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.ItemCategory;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.entity.master.CategoryEntity;
import com.erp.mapper.EntityMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {ItemBlueprintDataMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemBlueprintMapper extends EntityMapper<ItemBlueprintEntity, NewItem, Item> {

  @Mapping(source = "category", target = "category")
  @Override
  Item toDomain(ItemBlueprintEntity entity);

  @Mapping(target = "category", ignore = true)
  @Override
  ItemBlueprintEntity toEntity(NewItem dto);

  @Mapping(target = "category", ignore = true)
  @Override
  void updateEntity(@MappingTarget ItemBlueprintEntity entity, NewItem dto);

  List<Item> toDomainList(List<ItemBlueprintEntity> entities);

  default ItemCategory toItemCategory(CategoryEntity entity) {
    if (entity == null) return null;
    ItemCategory result = new ItemCategory();
    result.setId(entity.getId());
    result.setName(entity.getName());
    return result;
  }
}

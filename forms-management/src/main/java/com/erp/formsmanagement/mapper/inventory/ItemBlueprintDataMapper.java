package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.Size;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemBlueprintDataMapper {
  Size toDomain(ItemBlueprintDataEntity entity);
  List<Size> toDomainList(List<ItemBlueprintDataEntity> entities);
  ItemBlueprintDataEntity toEntity(com.erp.api.itemmanagement.model.NewSize dto);
  void updateEntityFromDto(com.erp.api.itemmanagement.model.NewSize dto, @org.mapstruct.MappingTarget ItemBlueprintDataEntity entity);
}

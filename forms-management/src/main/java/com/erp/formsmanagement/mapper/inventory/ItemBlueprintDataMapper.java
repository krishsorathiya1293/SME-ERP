package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.mapper.EntityMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemBlueprintDataMapper extends EntityMapper<ItemBlueprintDataEntity, NewSize, Size> {

  @Override
  Size toDomain(ItemBlueprintDataEntity entity);

  @Override
  ItemBlueprintDataEntity toEntity(NewSize dto);

  @Override
  void updateEntity(@MappingTarget ItemBlueprintDataEntity entity, NewSize dto);

  List<Size> toDomainList(List<ItemBlueprintDataEntity> entities);
}

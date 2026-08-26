package com.erp.formsmanagement.mapper.inventory;

import com.erp.api.itemmanagement.model.HighlightColor;
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

  /**
   * The column stores the colour the way the API spells it — lower case, "rose" — but MapStruct's
   * default String-to-enum conversion is {@code Enum.valueOf}, which wants the constant name
   * ("ROSE") and blew up on every marked row. Go through the generated {@code fromValue} instead,
   * which is the one that knows the wire spelling.
   *
   * <p>A name that is no longer in the palette reads as unmarked rather than throwing: one stale
   * row should not take the whole stock master down with it.
   */
  default HighlightColor toHighlightColor(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    for (HighlightColor color : HighlightColor.values()) {
      if (color.getValue().equalsIgnoreCase(value.trim())) {
        return color;
      }
    }
    return null;
  }
}

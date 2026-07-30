package com.erp.formsmanagement.mapper.gres;

import com.erp.api.gresmanagement.model.GresFillingItem;
import com.erp.api.gresmanagement.model.GresFillingSize;
import com.erp.api.gresmanagement.model.NewGresFillingItem;
import com.erp.formsmanagement.domain.entity.gres.GresFillingItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface GresFillingItemMapper {

  @Mapping(target = "size", expression = "java(toSize(entity))")
  GresFillingItem toDomain(GresFillingItemEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "gresFilling", ignore = true)
  @Mapping(target = "size", ignore = true)
  GresFillingItemEntity toEntity(NewGresFillingItem request);

  default GresFillingSize toSize(GresFillingItemEntity entity) {
    if (entity.getSize() == null) return null;
    var sz = entity.getSize();
    GresFillingSize s = new GresFillingSize();
    s.setId(sz.getId());
    s.setSizeInInch(sz.getSizeInInch());
    s.setSizeInMm(sz.getSizeInMm());
    if (sz.getItem() != null) {
      s.setItemId(sz.getItem().getId());
      s.setItemName(sz.getItem().getItemName());
    }
    return s;
  }
}

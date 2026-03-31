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
    GresFillingSize s = new GresFillingSize();
    s.setId(entity.getSize().getId());
    s.setSizeInInch(entity.getSize().getSizeInInch());
    s.setSizeInMm(entity.getSize().getSizeInMm());
    return s;
  }
}

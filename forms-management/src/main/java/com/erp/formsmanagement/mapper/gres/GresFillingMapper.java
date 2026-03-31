package com.erp.formsmanagement.mapper.gres;

import com.erp.api.gresmanagement.model.GresFilling;
import com.erp.api.gresmanagement.model.GresFillingParty;
import com.erp.api.gresmanagement.model.NewGresFilling;
import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    uses = {GresFillingItemMapper.class, GresFillingReturnMapper.class})
public interface GresFillingMapper extends EntityMapper<GresFillingEntity, NewGresFilling, GresFilling> {

  @Override
  @Mapping(target = "party", expression = "java(toParty(entity))")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "lastUpdatedAt", source = "lastUpdatedAt")
  GresFilling toDomain(GresFillingEntity entity);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "items", ignore = true)
  @Mapping(target = "returns", ignore = true)
  GresFillingEntity toEntity(NewGresFilling request);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "items", ignore = true)
  @Mapping(target = "returns", ignore = true)
  void updateEntity(@MappingTarget GresFillingEntity entity, NewGresFilling request);

  default GresFillingParty toParty(GresFillingEntity entity) {
    if (entity.getParty() == null) return null;
    GresFillingParty p = new GresFillingParty();
    p.setId(entity.getParty().getId());
    p.setName(entity.getParty().getName());
    return p;
  }
}

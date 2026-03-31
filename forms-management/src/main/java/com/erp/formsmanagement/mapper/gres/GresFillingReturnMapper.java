package com.erp.formsmanagement.mapper.gres;

import com.erp.api.gresmanagement.model.GresFillingReturn;
import com.erp.api.gresmanagement.model.NewGresFillingReturn;
import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.mapper.EntityMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface GresFillingReturnMapper
    extends EntityMapper<GresFillingReturnEntity, NewGresFillingReturn, GresFillingReturn> {

  @Override
  @Mapping(target = "createdAt", source = "createdAt")
  GresFillingReturn toDomain(GresFillingReturnEntity entity);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "gresFilling", ignore = true)
  GresFillingReturnEntity toEntity(NewGresFillingReturn request);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "gresFilling", ignore = true)
  void updateEntity(@MappingTarget GresFillingReturnEntity entity, NewGresFillingReturn request);

  default OffsetDateTime map(LocalDateTime value) {
    if (value == null) return null;
    return value.atOffset(ZoneOffset.UTC);
  }
}

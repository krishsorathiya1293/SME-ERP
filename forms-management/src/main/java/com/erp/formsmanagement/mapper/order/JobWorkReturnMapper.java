package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.JobWorkReturn;
import com.erp.api.ordermanagement.model.NewJobWorkReturn;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.mapper.EntityMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface JobWorkReturnMapper
    extends EntityMapper<JobWorkReturnEntity, NewJobWorkReturn, JobWorkReturn> {

  @Override
  JobWorkReturn toDomain(JobWorkReturnEntity entity);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "jobWork", ignore = true)
  JobWorkReturnEntity toEntity(NewJobWorkReturn request);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "jobWork", ignore = true)
  void updateEntity(@MappingTarget JobWorkReturnEntity entity, NewJobWorkReturn request);

  default OffsetDateTime map(LocalDateTime value) {
    if (value == null) return null;
    return value.atOffset(ZoneOffset.UTC);
  }
}

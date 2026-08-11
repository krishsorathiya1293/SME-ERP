package com.erp.formsmanagement.mapper.order;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.JobWorkParty;
import com.erp.api.ordermanagement.model.JobWorkSize;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.util.JobWorkNumber;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE, uses = {JobWorkReturnMapper.class})
public interface JobWorkMapper extends EntityMapper<JobWorkEntity, NewJobWork, JobWork> {

  @Override
  @Mapping(target = "orderItemId", expression = "java(toOrderItemId(entity))")
  @Mapping(target = "party", expression = "java(toParty(entity))")
  @Mapping(target = "size", expression = "java(toSize(entity))")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "jobWorkLabel", expression = "java(toJobWorkLabel(entity))")
  JobWork toDomain(JobWorkEntity entity);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "orderItem", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "jobWorkReturns", ignore = true)
  JobWorkEntity toEntity(NewJobWork request);

  @Override
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "orderItem", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "jobWorkReturns", ignore = true)
  void updateEntity(@MappingTarget JobWorkEntity entity, NewJobWork request);

  // ---------- Custom Mappers ----------

  /**
   * Owning order item id, or null for manual job works. Reads only the id off the (possibly lazy)
   * association, so it never triggers an extra select — the FK already lives on the job_works row.
   */
  default Long toOrderItemId(JobWorkEntity entity) {
    return entity.getOrderItem() != null ? entity.getOrderItem().getId() : null;
  }

  default JobWorkParty toParty(JobWorkEntity entity) {
    if (entity.getParty() == null) return null;
    JobWorkParty p = new JobWorkParty();
    p.setId(entity.getParty().getId());
    p.setName(entity.getParty().getName());
    return p;
  }

  default JobWorkSize toSize(JobWorkEntity entity) {
    return SizeContextMapper.toJobWorkSize(entity.getSize());
  }

  default String toJobWorkLabel(JobWorkEntity entity) {
    String partyName = entity.getParty() != null ? entity.getParty().getName() : null;
    return JobWorkNumber.label(partyName, entity.getJobWorkNo());
  }

}

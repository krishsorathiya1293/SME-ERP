package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.repository.CoreRepository;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobWorkReturnRepository
    extends CoreRepository<JobWorkReturnEntity, Long> {

  default Specification<JobWorkReturnEntity> filterByJobWorkId(Long jobWorkId) {
    return (root, query, cb) ->
        jobWorkId == null ? null : cb.equal(root.get("jobWork").get("id"), jobWorkId);
  }

  default Specification<JobWorkReturnEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .filter(s -> !s.isBlank())
            .map(s -> cb.like(cb.lower(root.get("elementType").as(String.class)),
                "%" + s.toLowerCase() + "%"))
            .orElse(null);
  }

  @Query("SELECT COALESCE(SUM(COALESCE(r.returnKg, 0.0) + COALESCE(r.ghati, 0.0)), 0.0) FROM JobWorkReturnEntity r WHERE r.jobWork.id = :jobWorkId AND r.id <> :excludeId")
  Double sumReturnKgByJobWorkId(@Param("jobWorkId") Long jobWorkId, @Param("excludeId") Long excludeId);

  @Query("SELECT COALESCE(SUM(r.returnElementCount), 0.0) FROM JobWorkReturnEntity r WHERE r.jobWork.id = :jobWorkId AND r.id <> :excludeId")
  Double sumReturnElementCountByJobWorkId(@Param("jobWorkId") Long jobWorkId, @Param("excludeId") Long excludeId);
}

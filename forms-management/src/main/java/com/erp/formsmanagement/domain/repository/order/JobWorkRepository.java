package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkStatus;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.erp.repository.CoreRepository;

public interface JobWorkRepository extends CoreRepository<JobWorkEntity, Long> {

  /**
   * Highest job work number assigned to the given party within [startDate, endDate). Used to derive
   * the next per-party, per-month sequence number (max + 1).
   */
  @Query(
"""
    SELECT COALESCE(MAX(jw.jobWorkNo), 0)
    FROM JobWorkEntity jw
    WHERE jw.party.id = :partyId
      AND jw.createdAt >= :startDate
      AND jw.createdAt < :endDate
""")
  Integer findMaxJobWorkNoForPartyAndMonth(
      @Param("partyId") Long partyId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  default Specification<JobWorkEntity> filterByOrderItemId(Long orderItemId) {
    return (root, query, cb) ->
        orderItemId == null ? null : cb.equal(root.get("orderItem").get("id"), orderItemId);
  }

  default Specification<JobWorkEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .filter(s -> !s.isBlank())
            .map(
                s -> {
                  String like = "%" + s.toLowerCase() + "%";
                  // party + size are non-null ManyToOne, so these are inner joins — no rows dropped.
                  return cb.or(
                      cb.like(cb.lower(root.get("party").get("name")), like),
                      cb.like(cb.lower(root.get("finish")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInInch")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInMm")), like));
                })
            .orElse(null);
  }

  default Specification<JobWorkEntity> filterByType(JobWorkType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("jobWorkType"), type);
  }

  default Specification<JobWorkEntity> filterByStatus(JobWorkStatus status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  List<JobWorkEntity> findByPartyIdAndJobDateBetweenOrderByJobDateAsc(
      Long partyId, LocalDate startDate, LocalDate endDate);
}

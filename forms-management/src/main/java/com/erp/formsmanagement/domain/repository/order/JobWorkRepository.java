package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
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
                s ->
                    cb.or(
                        cb.like(
                            cb.lower(root.get("party").get("name")),
                            "%" + s.toLowerCase() + "%"),
                        cb.like(
                            cb.lower(root.get("finish")), "%" + s.toLowerCase() + "%")))
            .orElse(null);
  }

  List<JobWorkEntity> findByPartyIdAndJobDateBetweenOrderByJobDateAsc(
      Long partyId, LocalDate startDate, LocalDate endDate);
}

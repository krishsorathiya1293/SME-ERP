package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnState;
import com.erp.formsmanagement.domain.entity.order.JobWorkStatus;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
                  // chitthiNo is nullable; a null column simply never matches the LIKE.
                  return cb.or(
                      cb.like(cb.lower(root.get("party").get("name")), like),
                      cb.like(cb.lower(root.get("finish")), like),
                      cb.like(cb.lower(root.get("chitthiNo")), like),
                      cb.like(cb.lower(root.get("jobWorkLabel")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInInch")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInMm")), like),
                      cb.like(
                          cb.lower(root.get("size").get("item").get("itemName")),
                          like));
                })
            .orElse(null);
  }

  default Specification<JobWorkEntity> filterByType(JobWorkType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("jobWorkType"), type);
  }

  default Specification<JobWorkEntity> filterByStatus(JobWorkStatus status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  /**
   * Narrows to how much of each chitthi has come back. The returned weight is summed in a
   * correlated subquery rather than a join, so the row count stays one-per-job-work and the same
   * specification can drive both the paginated listing and the stat-card counts.
   *
   * <p>A job work that went out with no weight recorded ({@code qtyKg} null or 0) can never be
   * "fully returned" by arithmetic, so it stays in Pending until something comes back.
   */
  default Specification<JobWorkEntity> filterByReturnState(JobWorkReturnState state) {
    return (root, query, cb) -> {
      if (state == null) return null;

      Subquery<Double> returned = query.subquery(Double.class);
      Root<JobWorkReturnEntity> ret = returned.from(JobWorkReturnEntity.class);
      returned
          .select(
              cb.sum(
                  cb.sum(
                      cb.coalesce(ret.get("returnKg"), 0.0), cb.coalesce(ret.get("ghati"), 0.0))))
          .where(cb.equal(ret.get("jobWork"), root));

      Expression<Double> returnedKg = cb.coalesce(returned, 0.0);
      Expression<Double> sentKg = cb.coalesce(root.get("qtyKg"), 0.0);

      return switch (state) {
        case PENDING -> cb.lessThanOrEqualTo(returnedKg, 0.0);
        case PARTIALLY_RETURNED ->
            cb.and(cb.greaterThan(returnedKg, 0.0), cb.lessThan(returnedKg, sentKg));
        case FULLY_RETURNED ->
            cb.and(cb.greaterThan(returnedKg, 0.0), cb.greaterThanOrEqualTo(returnedKg, sentKg));
      };
    };
  }

  List<JobWorkEntity> findByPartyIdAndJobDateBetweenOrderByJobDateAsc(
      Long partyId, LocalDate startDate, LocalDate endDate);
}

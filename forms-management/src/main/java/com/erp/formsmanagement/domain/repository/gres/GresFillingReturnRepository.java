package com.erp.formsmanagement.domain.repository.gres;

import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GresFillingReturnRepository extends CoreRepository<GresFillingReturnEntity, Long> {

  default Specification<GresFillingReturnEntity> filterByGresFillingId(Long gresFillingId) {
    return (root, query, cb) ->
        gresFillingId == null ? null : cb.equal(root.get("gresFilling").get("id"), gresFillingId);
  }

  default Specification<GresFillingReturnEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .filter(s -> !s.isBlank())
            .map(
                s ->
                    cb.like(
                        cb.lower(root.get("elementType").as(String.class)),
                        "%" + s.toLowerCase() + "%"))
            .orElse(null);
  }

  /**
   * Total net Kg already returned against a Gres record, excluding one row (used
   * when editing so the row's own contribution isn't double-counted).
   */
  @Query(
      "SELECT COALESCE(SUM(r.returnKg), 0) FROM GresFillingReturnEntity r "
          + "WHERE r.gresFilling.id = :gresFillingId AND (:excludeId = 0 OR r.id <> :excludeId)")
  double sumReturnKgByGresFillingId(
      @Param("gresFillingId") Long gresFillingId, @Param("excludeId") Long excludeId);
}

package com.erp.formsmanagement.domain.repository.gres;

import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

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
}

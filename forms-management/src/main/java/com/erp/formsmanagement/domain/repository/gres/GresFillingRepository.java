package com.erp.formsmanagement.domain.repository.gres;

import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public interface GresFillingRepository extends CoreRepository<GresFillingEntity, Long> {

  default Specification<GresFillingEntity> filterBySearch(Optional<String> search) {
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
                            cb.lower(root.get("chitthiNo")), "%" + s.toLowerCase() + "%")))
            .orElse(null);
  }

  List<GresFillingEntity> findByPartyIdAndChitthiDateBetweenOrderByChitthiDateAsc(
      Long partyId, java.time.LocalDate startDate, java.time.LocalDate endDate);
}

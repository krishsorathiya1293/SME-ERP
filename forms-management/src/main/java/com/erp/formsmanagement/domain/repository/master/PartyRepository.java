package com.erp.formsmanagement.domain.repository.master;

import com.erp.api.mastermanagement.model.PartyType;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends CoreRepository<PartyEntity, Long> {
  default Specification<PartyEntity> byPartyType(Optional<String> partyType) {
    return (root, query, cb) ->
        partyType
            .map(p -> cb.equal(root.get("partyType"), PartyType.fromValue(p)))
            .orElse(cb.conjunction());
  }

  default Specification<PartyEntity> nameLike(String search) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
  }

  default Specification<PartyEntity> filter(String partyType, String search) {
    Specification<PartyEntity> spec = Specification.where(null);

    if (partyType != null && !partyType.isBlank()) {
      spec = spec.and(byPartyType(partyType.describeConstable()));
    }

    if (search != null && !search.isBlank()) {
      spec = spec.and(nameLike(search));
    }

    return spec;
  }
}

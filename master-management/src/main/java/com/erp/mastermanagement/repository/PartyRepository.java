package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.PartyEntity;
import com.erp.repository.CoreRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends CoreRepository<PartyEntity, Long> {
  default Specification<PartyEntity> byPartyType(String partyType) {
    return (root, query, cb) -> cb.equal(root.get("partyType").get("name"), partyType);
  }

  default Specification<PartyEntity> nameLike(String search) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
  }

  default Specification<PartyEntity> filter(String partyType, String search) {
    Specification<PartyEntity> spec = Specification.where(null);

    if (partyType != null && !partyType.isBlank()) {
      spec = spec.and(byPartyType(partyType));
    }

    if (search != null && !search.isBlank()) {
      spec = spec.and(nameLike(search));
    }

    return spec;
  }
}

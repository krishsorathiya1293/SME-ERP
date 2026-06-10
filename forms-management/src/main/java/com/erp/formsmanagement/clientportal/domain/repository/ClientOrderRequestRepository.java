package com.erp.formsmanagement.clientportal.domain.repository;

import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestStatus;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ClientOrderRequestRepository extends CoreRepository<ClientOrderRequestEntity, Long> {

  Page<ClientOrderRequestEntity> findByParty_Id(Long partyId, Pageable pageable);

  default Specification<ClientOrderRequestEntity> filter(
      Optional<Long> partyId, Optional<ClientOrderRequestStatus> status, Optional<String> search) {
    return (root, query, cb) -> {
      var predicate = cb.conjunction();

      if (partyId != null && partyId.isPresent()) {
        predicate = cb.and(predicate, cb.equal(root.get("party").get("id"), partyId.get()));
      }

      if (status != null && status.isPresent()) {
        predicate = cb.and(predicate, cb.equal(root.get("status"), status.get()));
      }

      if (search != null && search.isPresent() && !search.get().isBlank()) {
        String like = "%" + search.get().toLowerCase() + "%";
        predicate = cb.and(predicate, cb.like(cb.lower(root.join("party").get("name")), like));
      }

      return predicate;
    };
  }
}

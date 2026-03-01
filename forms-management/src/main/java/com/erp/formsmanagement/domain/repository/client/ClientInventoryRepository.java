package com.erp.formsmanagement.domain.repository.client;

import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public interface ClientInventoryRepository extends CoreRepository<ClientInventoryEntity, Long> {

  List<ClientInventoryEntity> findByParty_Id(Long partyId);

  Optional<ClientInventoryEntity> findByIdAndParty_Id(Long id, Long partyId);

  Optional<ClientInventoryEntity> findByParty_IdAndSize_Id(Long partyId, Long sizeId);

  default Specification<ClientInventoryEntity> filterByClientId(Long clientId) {
    return (root, query, cb) -> cb.equal(root.get("party").get("id"), clientId);
  }

  default Specification<ClientInventoryEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .filter(s -> !s.isBlank())
            .map(s -> {
              String like = "%" + s.toLowerCase() + "%";
              return cb.or(
                  cb.like(cb.lower(root.get("size").get("sizeInInch")), like),
                  cb.like(cb.lower(root.get("size").get("sizeInMm")), like),
                  cb.like(cb.lower(root.get("size").get("item").get("itemName")), like));
            })
            .orElse(cb.conjunction());
  }
}

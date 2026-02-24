package com.erp.formsmanagement.domain.repository.master;

import com.erp.api.mastermanagement.model.StockStatus;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends CoreRepository<ItemEntity, Long> {
  default Specification<ItemEntity> filterByStatus(Optional<String> status) {
    return (root, query, cb) ->
        status
            .map(s -> cb.equal(root.get("stockStatus"), StockStatus.fromValue(s)))
            .orElse(cb.conjunction());
  }

  default Specification<ItemEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .map(
                s -> {
                  String like = "%" + s.toLowerCase() + "%";
                  return cb.or(
                      cb.like(cb.lower(root.get("sizeInch")), like),
                      cb.like(cb.lower(root.get("sizeMm")), like));
                })
            .orElse(cb.conjunction());
  }
}

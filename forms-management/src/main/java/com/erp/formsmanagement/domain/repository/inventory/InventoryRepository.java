package com.erp.formsmanagement.domain.repository.inventory;

import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.filter.InventoryFilter;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public interface InventoryRepository extends CoreRepository<InventoryEntity, Long> {

  default Specification<InventoryEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) ->
        search
            .map(
                s -> {
                  String like = "%" + s.toLowerCase() + "%";
                  return cb.or(
                      cb.like(cb.lower(root.get("size").get("item").get("itemName")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInInch")), like),
                      cb.like(cb.lower(root.get("size").get("sizeInMm")), like));
                })
            .orElse(cb.conjunction());
  }

  default Specification<InventoryEntity> filterByInventoryFilter(Optional<InventoryFilter> filter) {
    return (root, query, cb) ->
        filter
            .map(
                f -> {
                  var predicates = cb.conjunction();
                  if (f.itemName() != null && !f.itemName().isBlank()) {
                    predicates =
                        cb.and(
                            predicates,
                            cb.like(
                                cb.lower(root.get("size").get("item").get("itemName")),
                                "%" + f.itemName().toLowerCase() + "%"));
                  }
                  if (f.sizeInInch() != null && !f.sizeInInch().isBlank()) {
                    predicates =
                        cb.and(
                            predicates,
                            cb.like(
                                cb.lower(root.get("size").get("sizeInInch")),
                                "%" + f.sizeInInch().toLowerCase() + "%"));
                  }
                  if (f.sizeInMm() != null && !f.sizeInMm().isBlank()) {
                    predicates =
                        cb.and(
                            predicates,
                            cb.like(
                                cb.lower(root.get("size").get("sizeInMm")),
                                "%" + f.sizeInMm().toLowerCase() + "%"));
                  }
                  return predicates;
                })
            .orElse(cb.conjunction());
  }

  default Specification<InventoryEntity> filterByItemId(Long itemId) {
    return (root, query, cb) -> cb.equal(root.get("size").get("item").get("id"), itemId);
  }
}

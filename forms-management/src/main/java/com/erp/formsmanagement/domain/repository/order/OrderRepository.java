package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.repository.CoreRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface OrderRepository extends CoreRepository<OrderEntity, Long> {

  default Specification<OrderEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) -> {
      if (search.isEmpty() || search.get().isBlank()) {
        return cb.conjunction();
      }
      String like = "%" + search.get().toLowerCase() + "%";
      return cb.like(cb.lower(root.join("party").get("name")), like);
    };
  }

  /**
   * Hides orders that have been folded into a merged one.
   *
   * <p>The merged order stands in for its sources everywhere the book is read — listing both would
   * show the same goods twice and double every total on the sheet. The sources are still there,
   * untouched, for the client portal and for un-merging.
   */
  default Specification<OrderEntity> notMergedAway() {
    return (root, query, cb) -> cb.isNull(root.get("mergedInto"));
  }

  List<OrderEntity> findByParty_IdInAndMergedIntoIsNull(Collection<Long> partyIds);

  Page<OrderEntity> findByParty_IdAndMergedIntoIsNull(Long partyId, Pageable pageable);

  /**
   * The client's own view, which is the mirror image of the admin's: the client placed two orders
   * and should keep seeing two. Merging them is the works' arrangement for getting the goods
   * plated, so it is the merged order that is hidden here, not the orders it was made from.
   */
  Page<OrderEntity> findByParty_IdAndMergedSourcesIsEmpty(Long partyId, Pageable pageable);
}

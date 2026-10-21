package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.repository.CoreRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Optional;
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
   * Hides orders whose every line has been folded into a merged line.
   *
   * <p>The merged line stands in for its sources everywhere the book is read — listing both would
   * show the same goods twice and double every total on the sheet. An order with only some lines
   * merged stays, showing the rest; its merged lines are left out when it is mapped. The sources
   * are still there, untouched, for the client portal and for un-merging.
   *
   * <p>{@code mergedInto} is what orders merged before lines could be merged on their own were
   * linked by; every line of such an order is folded too, so it is hidden either way.
   */
  default Specification<OrderEntity> notMergedAway() {
    return (root, query, cb) -> {
      Subquery<Long> visibleLine = query.subquery(Long.class);
      Root<OrderItemEntity> line = visibleLine.from(OrderItemEntity.class);
      visibleLine
          .select(line.get("id"))
          .where(cb.equal(line.get("order"), root), cb.isNull(line.get("mergedIntoItem")));

      Subquery<Long> anyLine = query.subquery(Long.class);
      Root<OrderItemEntity> any = anyLine.from(OrderItemEntity.class);
      anyLine.select(any.get("id")).where(cb.equal(any.get("order"), root));

      return cb.and(
          cb.isNull(root.get("mergedInto")),
          cb.or(cb.exists(visibleLine), cb.not(cb.exists(anyLine))));
    };
  }

  /**
   * The client's own view, which is the mirror image of the admin's: the client placed two orders
   * and should keep seeing two. Merging them is the works' arrangement for getting the goods
   * plated, so it is the merged order that is hidden here, not the orders it was made from.
   */
  default Specification<OrderEntity> notAMergedOrder() {
    return (root, query, cb) -> {
      Subquery<Long> foldedIn = query.subquery(Long.class);
      Root<OrderItemEntity> source = foldedIn.from(OrderItemEntity.class);
      foldedIn
          .select(source.get("id"))
          .where(cb.equal(source.get("mergedIntoItem").get("order"), root));
      return cb.and(cb.isEmpty(root.get("mergedSources")), cb.not(cb.exists(foldedIn)));
    };
  }
}

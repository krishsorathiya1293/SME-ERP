package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import com.erp.repository.CoreRepository;

public interface JobWorkRepository extends CoreRepository<JobWorkEntity, Long> {

  default Specification<JobWorkEntity> filterByOrderItemId(Long orderItemId) {
    return (root, query, cb) ->
        orderItemId == null ? null : cb.equal(root.get("orderItem").get("id"), orderItemId);
  }

  default Specification<JobWorkEntity> filterBySearch(Optional<String> search) {
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
                            cb.lower(root.get("finish")), "%" + s.toLowerCase() + "%")))
            .orElse(null);
  }
}

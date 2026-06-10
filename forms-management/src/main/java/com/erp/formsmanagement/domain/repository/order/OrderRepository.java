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

  List<OrderEntity> findByParty_IdIn(Collection<Long> partyIds);

  Page<OrderEntity> findByParty_Id(Long partyId, Pageable pageable);
}

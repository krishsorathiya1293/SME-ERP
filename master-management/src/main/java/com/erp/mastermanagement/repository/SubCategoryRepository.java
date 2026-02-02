package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.SubCategoryEntity;
import com.erp.repository.CoreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface SubCategoryRepository extends CoreRepository<SubCategoryEntity, Long> {

  default Specification<SubCategoryEntity> byCategoryAndName(Long categoryId, String search) {

    return (root, query, cb) -> {
      Predicate byCategory = cb.equal(root.get("category").get("id"), categoryId);

      if (search == null || search.isBlank()) {
        return byCategory;
      }

      Predicate byName = cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");

      return cb.and(byCategory, byName);
    };
  }
}

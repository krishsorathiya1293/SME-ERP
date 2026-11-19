package com.erp.formsmanagement.domain.repository.master;

import com.erp.api.mastermanagement.model.StockStatus;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.repository.CoreRepository;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends CoreRepository<ItemEntity, Long> {
  Optional<ItemEntity> findFirstBySize_IdOrderByIdAsc(Long sizeId);

  /**
   * The size's stock row, locked until the transaction ends. Every stock change reads through
   * this, so two changes to one size (a stock entry and a job work, or two job works) queue up
   * instead of both reading the same figure and one overwriting the other.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from ItemEntity i where i.size.id = :sizeId order by i.id asc")
  List<ItemEntity> lockBySizeId(@Param("sizeId") Long sizeId);

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
                  var sizeJoin = root.join("size", JoinType.LEFT);
                  return cb.or(
                      cb.like(cb.lower(sizeJoin.get("sizeInInch")), like),
                      cb.like(cb.lower(sizeJoin.get("sizeInMm")), like));
                })
            .orElse(cb.conjunction());
  }
}

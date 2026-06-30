package com.erp.formsmanagement.domain.repository.pricing;

import com.erp.formsmanagement.domain.entity.pricing.PricingRuleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingRuleRepository extends JpaRepository<PricingRuleEntity, Long> {

  /**
   * All rules that could apply to the given scope: the exact client/item plus the broader
   * fallbacks (client-only, item-only, global). The service then picks the most specific per
   * finish. A null clientId/itemId simply restricts candidates to the null (global) rows.
   */
  @Query(
      "select p from PricingRuleEntity p "
          + "where (p.clientId = :clientId or p.clientId is null) "
          + "and (p.itemId = :itemId or p.itemId is null)")
  List<PricingRuleEntity> findCandidates(
      @Param("clientId") Long clientId, @Param("itemId") Long itemId);

  /** Exact-scope lookup for upsert/delete (null-safe on both ids). */
  @Query(
      "select p from PricingRuleEntity p "
          + "where ((:clientId is null and p.clientId is null) or p.clientId = :clientId) "
          + "and ((:itemId is null and p.itemId is null) or p.itemId = :itemId) "
          + "and p.finishKey = :finishKey")
  Optional<PricingRuleEntity> findExact(
      @Param("clientId") Long clientId,
      @Param("itemId") Long itemId,
      @Param("finishKey") String finishKey);
}

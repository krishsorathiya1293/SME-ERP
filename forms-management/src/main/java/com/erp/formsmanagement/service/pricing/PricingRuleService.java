package com.erp.formsmanagement.service.pricing;

import java.util.List;

public interface PricingRuleService {

  /** The 12 derived finish columns whose price is computed from S.S. */
  List<String> FINISH_KEYS =
      List.of(
          "antiq", "sidegold", "sartinlacq", "zblack", "grblack", "mattss", "mattantiq", "pvdrose",
          "pvdgold", "pvdblack", "rosegold", "clearlacq");

  /**
   * Resolved formula per finish for the given scope, applying precedence
   * client+item &rarr; client &rarr; item &rarr; global. Always returns a rule for every finish
   * (falling back to the seeded global default).
   */
  List<ResolvedPricingRule> resolve(Long clientId, Long itemId);

  /** Create or update the rule at the exact (clientId, itemId, finishKey) scope. */
  ResolvedPricingRule upsert(
      Long clientId, Long itemId, String finishKey, double multiplier, double offset);

  /** Remove an override at the exact scope so it falls back to the parent scope. */
  void delete(Long clientId, Long itemId, String finishKey);
}

package com.erp.formsmanagement.service.pricing.impl;

import com.erp.formsmanagement.domain.entity.pricing.PricingRuleEntity;
import com.erp.formsmanagement.domain.repository.pricing.PricingRuleRepository;
import com.erp.formsmanagement.service.pricing.PricingRuleService;
import com.erp.formsmanagement.service.pricing.ResolvedPricingRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PricingRuleServiceImpl implements PricingRuleService {

  private final PricingRuleRepository pricingRuleRepository;

  public PricingRuleServiceImpl(PricingRuleRepository pricingRuleRepository) {
    this.pricingRuleRepository = pricingRuleRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ResolvedPricingRule> resolve(Long clientId, Long itemId) {
    List<PricingRuleEntity> candidates =
        pricingRuleRepository.findCandidates(clientId, itemId);

    // Keep the most specific candidate per finish key.
    Map<String, PricingRuleEntity> best = new HashMap<>();
    for (PricingRuleEntity rule : candidates) {
      PricingRuleEntity current = best.get(rule.getFinishKey());
      if (current == null || specificity(rule) > specificity(current)) {
        best.put(rule.getFinishKey(), rule);
      }
    }

    List<ResolvedPricingRule> resolved = new ArrayList<>();
    for (String finishKey : FINISH_KEYS) {
      PricingRuleEntity rule = best.get(finishKey);
      if (rule != null) {
        resolved.add(
            new ResolvedPricingRule(finishKey, rule.getMultiplier(), rule.getOffsetValue()));
      }
    }
    return resolved;
  }

  @Override
  public ResolvedPricingRule upsert(
      Long clientId, Long itemId, String finishKey, double multiplier, double offset) {
    PricingRuleEntity entity =
        pricingRuleRepository
            .findExact(clientId, itemId, finishKey)
            .orElseGet(PricingRuleEntity::new);
    entity.setClientId(clientId);
    entity.setItemId(itemId);
    entity.setFinishKey(finishKey);
    entity.setMultiplier(multiplier);
    entity.setOffsetValue(offset);
    PricingRuleEntity saved = pricingRuleRepository.save(entity);
    return new ResolvedPricingRule(
        saved.getFinishKey(), saved.getMultiplier(), saved.getOffsetValue());
  }

  @Override
  public void delete(Long clientId, Long itemId, String finishKey) {
    pricingRuleRepository
        .findExact(clientId, itemId, finishKey)
        .ifPresent(pricingRuleRepository::delete);
  }

  /** Higher = more specific. client+item (3) &gt; client (2) &gt; item (1) &gt; global (0). */
  private int specificity(PricingRuleEntity rule) {
    return (rule.getClientId() != null ? 2 : 0) + (rule.getItemId() != null ? 1 : 0);
  }
}

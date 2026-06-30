package com.erp.formsmanagement.controller.pricing;

import com.erp.formsmanagement.service.pricing.PricingRuleService;
import com.erp.formsmanagement.service.pricing.ResolvedPricingRule;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingRuleController {

  private final PricingRuleService pricingRuleService;

  public PricingRuleController(PricingRuleService pricingRuleService) {
    this.pricingRuleService = pricingRuleService;
  }

  /** Resolved finish formulas for a scope. Omit clientId/itemId for the global defaults. */
  @GetMapping("/api/v1/pricing-rules/resolve")
  public ResponseEntity<List<ResolvedPricingRule>> resolve(
      @RequestParam(required = false) Long clientId,
      @RequestParam(required = false) Long itemId) {
    return ResponseEntity.ok(pricingRuleService.resolve(clientId, itemId));
  }

  /** Create or update a single finish formula at the given scope. */
  @PutMapping("/api/v1/pricing-rules")
  public ResponseEntity<ResolvedPricingRule> upsert(@RequestBody PricingRuleRequest request) {
    return ResponseEntity.ok(
        pricingRuleService.upsert(
            request.clientId(),
            request.itemId(),
            request.finishKey(),
            request.multiplier() != null ? request.multiplier() : 1.0,
            request.offset() != null ? request.offset() : 0.0));
  }

  /** Clear an override so the scope falls back to its parent. */
  @DeleteMapping("/api/v1/pricing-rules")
  public ResponseEntity<Void> delete(
      @RequestParam(required = false) Long clientId,
      @RequestParam(required = false) Long itemId,
      @RequestParam String finishKey) {
    pricingRuleService.delete(clientId, itemId, finishKey);
    return ResponseEntity.noContent().build();
  }

  public record PricingRuleRequest(
      Long clientId, Long itemId, String finishKey, Double multiplier, Double offset) {}
}

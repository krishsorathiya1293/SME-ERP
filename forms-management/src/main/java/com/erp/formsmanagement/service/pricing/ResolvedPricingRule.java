package com.erp.formsmanagement.service.pricing;

/** A resolved finish-price formula: {@code finish = ss * multiplier + offset}. */
public record ResolvedPricingRule(String finishKey, double multiplier, double offset) {}

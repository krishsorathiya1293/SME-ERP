package com.erp.formsmanagement.service.order;

/**
 * Global job-work counts for the stat cards. Kept separate from the paginated list so the cards
 * stay correct across the whole (optionally filtered) dataset, not just the current page.
 *
 * <p>The three buckets mirror the return-state filter — nothing back, some back, all back — and
 * therefore always add up to {@code total}.
 */
public record JobWorkStats(
    long total, long pending, long partiallyReturned, long fullyReturned) {}

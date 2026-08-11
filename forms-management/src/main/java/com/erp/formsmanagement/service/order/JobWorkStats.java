package com.erp.formsmanagement.service.order;

/**
 * Global job-work counts for the stat cards. Kept separate from the paginated list so the cards
 * stay correct across the whole (optionally filtered) dataset, not just the current page.
 */
public record JobWorkStats(long total, long completed, long pending) {}

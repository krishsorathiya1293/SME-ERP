package com.erp.formsmanagement.domain.entity.order;

/**
 * How much of a job work has come back from the plater. Derived from the return records rather
 * than stored: {@code JobWorkStatus} only ever says "a return exists", which cannot tell a chitthi
 * with 10 of 300 Kg back from one that is finished. The floor asks for exactly these three
 * buckets, so the filter is defined on the returned weight instead of on the status column.
 *
 * <p>Returned weight counts the net kg plus the ghati, matching the card's arithmetic.
 */
public enum JobWorkReturnState {
  /** Nothing has come back yet. */
  PENDING,
  /** Some weight is back, but less than what went out. */
  PARTIALLY_RETURNED,
  /** Everything that went out (or more) is back. */
  FULLY_RETURNED
}

package com.erp.formsmanagement.clientportal.domain.entity;

/**
 * Status lifecycle for a client order request submitted from the Client Portal "Shop".
 *
 * <p>The first three are set by hand by an admin (approve / reject). Everything from
 * {@link #IN_PLATING} onwards is derived from the works — the order created on approval moves
 * through job work and dispatch, and {@code ClientOrderFulfillmentService} writes the stage back
 * here so a request only ever shows in one tab.
 *
 * <p>{@link #IN_PROGRESS} is legacy: nothing derives it any more, but old rows may still carry it.
 */
public enum ClientOrderRequestStatus {
  PENDING_APPROVAL,
  APPROVED,
  IN_PLATING,
  READY_TO_DISPATCH,
  DISPATCHED,
  IN_PROGRESS,
  COMPLETED,
  REJECTED
}

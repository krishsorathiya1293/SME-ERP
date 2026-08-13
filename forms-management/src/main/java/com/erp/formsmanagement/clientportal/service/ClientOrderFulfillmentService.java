package com.erp.formsmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.OrderItemStage;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import java.util.Map;

/**
 * Keeps a client order request's status in step with what has actually happened to the order it
 * produced. An approved request becomes a real order; that order then goes out for plating, comes
 * back, and is dispatched. Each of those events writes the resulting stage back onto the request so
 * the admin's Order Approvals tabs stay mutually exclusive — an order out for plating shows under
 * "In Plating" and no longer under "Approved".
 */
public interface ClientOrderFulfillmentService {

  /** Per-line progress, used to show item-level detail alongside the request's rolled-up status. */
  record ItemFulfillment(
      OrderItemStage stage,
      Double sentKg,
      Double returnedKg,
      Double remainingKg,
      Double dispatchedPc) {}

  /**
   * Recomputes and persists the status of any client request behind this order item's order.
   * No-op for a job work that isn't tied to an order item (manual job work) or an order that no
   * client request produced.
   */
  void syncByOrderItem(OrderItemEntity orderItem);

  /** Recomputes and persists the status of any client request behind the given order. */
  void syncByOrderId(Long orderId);

  /**
   * Per-line progress for an order, keyed by {@link #lineKey}. Size + plating is the only stable
   * handle a client request item keeps on the order item it became — the same size can legitimately
   * appear twice in one order under two different finishes.
   *
   * <p>Also carries a size-only key per size, so a line whose plating was edited after approval
   * still resolves (to the least advanced item of that size).
   */
  Map<String, ItemFulfillment> describeByLine(OrderEntity order);

  /** Lookup key for {@link #describeByLine}; pass a null plating for the size-only fallback. */
  static String lineKey(Long sizeId, String plating) {
    return sizeId + "|" + (plating == null ? "" : plating.trim().toLowerCase());
  }
}

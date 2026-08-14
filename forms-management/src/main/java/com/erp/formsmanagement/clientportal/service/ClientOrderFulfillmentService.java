package com.erp.formsmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.OrderItemStage;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import java.util.Map;

/**
 * Works out where an order's quantity actually sits.
 *
 * <p>The stages are not buckets of orders — they are buckets of <em>quantity</em>. One line of 100
 * Kg is normally in several at once: send 50 Kg to the plater and 30 Kg comes back, and that line
 * reads approved 50, in plating 20, ready to dispatch 30. So the same order shows under several
 * tabs, each showing only the part of it that is really at that stage.
 *
 * <p>It also keeps the request-level {@code status} column in step, which is what the admin's Order
 * Approvals tabs still filter on.
 */
public interface ClientOrderFulfillmentService {

  /**
   * One line's quantity split across the stages, in Kg with the piece equivalent alongside.
   *
   * <p>On a line whose books balance, {@code approved + inPlating + readyToDispatch + dispatched +
   * ghati == ordered}. It can come out over where the works booked more movement than was ordered
   * — dispatching a whole line while part of it is still with the plater, say — because each figure
   * reports what was actually recorded rather than forcing the total to reconcile.
   *
   * @param stage the single furthest stage this line has reached — the coarse roll-up the admin
   *     screen shows; the Kg fields are what the client portal renders.
   */
  record ItemFulfillment(
      OrderItemStage stage,
      Double orderedKg,
      Double orderedPc,
      Double approvedKg,
      Double approvedPc,
      Double inPlatingKg,
      Double inPlatingPc,
      Double readyToDispatchKg,
      Double readyToDispatchPc,
      Double dispatchedKg,
      Double dispatchedPc,
      Double ghatiKg,
      Double sentKg,
      Double returnedKg,
      Double remainingKg) {}

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

  /** Per-line progress for an order, keyed by order-item id. */
  Map<Long, ItemFulfillment> describeByOrderItemId(OrderEntity order);

  /** Lookup key for {@link #describeByLine}; pass a null plating for the size-only fallback. */
  static String lineKey(Long sizeId, String plating) {
    return sizeId + "|" + (plating == null ? "" : plating.trim().toLowerCase());
  }
}

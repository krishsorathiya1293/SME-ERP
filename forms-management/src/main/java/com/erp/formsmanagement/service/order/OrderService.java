package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV1;
import com.erp.service.GetAllServiceV2;

public interface OrderService
    extends CoreServiceV2<Long, NewOrder, Order, Long>,
        GetAllServiceV2<Long, String, PaginatedResultOrder>,
        GetAllServiceV1<String, PaginatedPartyOrdersResponse> {

  /**
   * Deletes a single order item. Dependent job work, its returns, and dispatches cascade at the DB
   * level. If the item was the last one on its order, the (now empty) order is removed too.
   */
  void deleteItem(Long partyId, Long orderId, Long itemId);

  /**
   * Sets the scrap agreed for an order, on its own.
   *
   * <p>Separate from the ordinary update because the scrap is routinely settled after the order is
   * on the books — from the orders sheet, or when a client's request is approved — and putting it
   * through the full order update would mean resending every line to change one number.
   *
   * @param scrap the agreed amount, or null to clear it back to "not agreed yet"
   */
  Order updateScrap(Long orderId, Double scrap);

  /**
   * Ticks (or un-ticks) an order as manually completed, which takes it off the orders sheet.
   *
   * <p>Its own call rather than part of the order update for the same reason as the scrap: it is
   * set long after the order is on the books, by someone tidying a screen who has no business
   * resending every line to do it.
   */
  Order updateCompleted(Long orderId, boolean completed);

  /**
   * Ticks or un-ticks a single line, which is the level the sheet actually works at: five items on
   * one order finish at five different times.
   *
   * @return the parent order as it now reads, lines included
   */
  Order updateItemCompleted(Long orderItemId, boolean completed);

  /**
   * Folds order lines of one party together. Lines with the same size and plating become one
   * merged line, on a merged order.
   *
   * <p>Two lines for the same item, size and finish are one job on the floor — 100 Kg and 200 Kg
   * go into the same drum — and saying so once is what lets every chitthi and dispatch after it
   * follow without the merge being re-declared each time.
   *
   * <p>A line may already be at the plater or partly dispatched. Its chitthis and dispatches move
   * onto the merged line, and each move is remembered so un-merge can put it back. The source
   * lines keep their own quantities, so the client portal still reports each request against what
   * that request ordered. Completed lines cannot be merged.
   *
   * @param scrap the scrap for the merged order; null leaves it as it is
   * @return the merged order
   */
  Order mergeOrders(java.util.List<Long> orderItemIds, Double scrap);

  /**
   * Undoes a merge: every source line comes back with the dispatches and chitthis that moved off
   * it, and the merged order is removed.
   *
   * <p>Allowed only while nothing has been recorded on the merged order since the merge — no new
   * dispatch or chitthi, and none of the moved ones edited. Work recorded against the combined
   * quantity has no honest way of being divided back across the lines it came from.
   */
  void unmergeOrder(Long orderId);
}

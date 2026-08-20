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
   * Folds several orders of one party into a single merged order.
   *
   * <p>Two orders for the same item, size and finish are one job on the floor — 100 Kg and 200 Kg
   * go into the same drum — and saying so once, on the orders, is what lets every chitthi and
   * dispatch after it follow without the merge being re-declared each time.
   *
   * <p>A new order is created rather than one source being grown into: the sources keep every row
   * exactly as the party placed it, so the client portal still reports each request against what
   * that request ordered, and un-merging is a matter of dropping the merged order.
   *
   * <p>Only orders still {@code CREATED} may be merged. Once goods are with the plater or gone out,
   * the quantity on the line is no longer just a number on a sheet and adding it to another
   * order's would misreport work that has already happened.
   *
   * @param scrap the scrap for the merged order; null leaves it unset
   */
  Order mergeOrders(java.util.List<Long> orderIds, Double scrap);

  /**
   * Undoes a merge, putting the source orders back exactly as they were.
   *
   * <p>Allowed only while the merged order is still {@code CREATED}. Afterwards its lines carry
   * chitthis and dispatches that belong to the merged quantity, and there is no honest way to
   * divide that history back across the orders it came from.
   */
  void unmergeOrder(Long orderId);
}

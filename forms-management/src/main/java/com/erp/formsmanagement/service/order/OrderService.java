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
}

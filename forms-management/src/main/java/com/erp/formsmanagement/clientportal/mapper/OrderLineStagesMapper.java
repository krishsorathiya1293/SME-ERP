package com.erp.formsmanagement.clientportal.mapper;

import com.erp.api.clientportalmanagement.model.OrderLineStages;
import com.erp.api.clientportalmanagement.model.StageQuantity;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService.ItemFulfillment;

/**
 * Renders a line's stage split for the wire. Shared by the client's own order view and the admin's
 * order-request view so both describe a line the same way.
 */
public final class OrderLineStagesMapper {

  private OrderLineStagesMapper() {}

  public static OrderLineStages toStages(ItemFulfillment f) {
    if (f == null) {
      return null;
    }
    return new OrderLineStages()
        .ordered(quantity(f.orderedKg(), f.orderedPc()))
        .approved(quantity(f.approvedKg(), f.approvedPc()))
        .inPlating(quantity(f.inPlatingKg(), f.inPlatingPc()))
        .readyToDispatch(quantity(f.readyToDispatchKg(), f.readyToDispatchPc()))
        .dispatched(quantity(f.dispatchedKg(), f.dispatchedPc()))
        .ghatiKg(f.ghatiKg());
  }

  private static StageQuantity quantity(Double kg, Double pc) {
    return new StageQuantity().kg(kg).pc(pc);
  }
}

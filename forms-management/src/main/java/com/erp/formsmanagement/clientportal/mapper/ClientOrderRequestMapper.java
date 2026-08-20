package com.erp.formsmanagement.clientportal.mapper;

import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequestItem;
import com.erp.api.clientportalmanagement.model.OrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestItemEntity;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService.ItemFulfillment;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClientOrderRequestMapper {

  public OrderRequest toDomain(ClientOrderRequestEntity entity, String username) {
    return toDomain(entity, username, Map.of(), null);
  }

  /**
   * @param fulfillmentByLine per-line progress from the order this request became, keyed by {@link
   *     ClientOrderFulfillmentService#lineKey}. Empty for a request that hasn't been approved into
   *     an order yet.
   * @param scrap the scrap on that order. Read from the order rather than copied onto the request,
   *     so the approvals screen and the orders sheet can never drift apart on it.
   */
  public OrderRequest toDomain(
      ClientOrderRequestEntity entity,
      String username,
      Map<String, ItemFulfillment> fulfillmentByLine,
      Double scrap) {
    return new OrderRequest()
        .id(entity.getId())
        .partyId(entity.getParty() == null ? null : entity.getParty().getId())
        .partyName(entity.getParty() == null ? null : entity.getParty().getName())
        .username(username)
        .status(
            entity.getStatus() == null ? null : OrderRequestStatus.valueOf(entity.getStatus().name()))
        .orderDate(entity.getOrderDate())
        .orderId(entity.getOrder() == null ? null : entity.getOrder().getId())
        .scrap(scrap)
        .items(toItems(entity.getItems(), fulfillmentByLine))
        .createdAt(
            entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC));
  }

  private List<OrderRequestItem> toItems(
      List<ClientOrderRequestItemEntity> items, Map<String, ItemFulfillment> fulfillmentByLine) {
    if (items == null) {
      return List.of();
    }
    return items.stream().map(item -> toItem(item, fulfillmentByLine)).toList();
  }

  private OrderRequestItem toItem(
      ClientOrderRequestItemEntity item, Map<String, ItemFulfillment> fulfillmentByLine) {
    Long sizeId = item.getItemSize() == null ? null : item.getItemSize().getId();

    OrderRequestItem dto =
        new OrderRequestItem()
            .id(item.getId())
            .itemId(item.getItem() == null ? null : item.getItem().getId())
            .sizeId(sizeId)
            .itemName(item.getItemName())
            .category(item.getCategory())
            .sizeInInch(item.getSizeInInch())
            .sizeInMm(item.getSizeInMm())
            .plating(item.getPlating())
            .qtyPc(item.getQtyPc())
            .qtyKg(item.getQtyKg())
            .pendingPc(item.getPendingPc());

    ItemFulfillment fulfillment = null;
    if (sizeId != null) {
      fulfillment =
          fulfillmentByLine.getOrDefault(
              ClientOrderFulfillmentService.lineKey(sizeId, item.getPlating()),
              fulfillmentByLine.get(ClientOrderFulfillmentService.lineKey(sizeId, null)));
    }
    if (fulfillment != null) {
      dto.stage(fulfillment.stage())
          .sentKg(fulfillment.sentKg())
          .returnedKg(fulfillment.returnedKg())
          .remainingKg(fulfillment.remainingKg())
          .dispatchedPc(fulfillment.dispatchedPc())
          .stages(OrderLineStagesMapper.toStages(fulfillment))
          .pendingPc(
              fulfillment.dispatchedPc() != null && item.getQtyPc() != null
                  ? Math.max(0d, item.getQtyPc() - fulfillment.dispatchedPc())
                  : item.getPendingPc());
    }

    return dto;
  }
}

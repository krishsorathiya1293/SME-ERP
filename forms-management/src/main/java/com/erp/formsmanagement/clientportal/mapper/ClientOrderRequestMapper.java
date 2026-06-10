package com.erp.formsmanagement.clientportal.mapper;

import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequestItem;
import com.erp.api.clientportalmanagement.model.OrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestItemEntity;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClientOrderRequestMapper {

  public OrderRequest toDomain(ClientOrderRequestEntity entity, String username) {
    return new OrderRequest()
        .id(entity.getId())
        .partyId(entity.getParty() == null ? null : entity.getParty().getId())
        .partyName(entity.getParty() == null ? null : entity.getParty().getName())
        .username(username)
        .status(
            entity.getStatus() == null ? null : OrderRequestStatus.valueOf(entity.getStatus().name()))
        .orderDate(entity.getOrderDate())
        .orderId(entity.getOrder() == null ? null : entity.getOrder().getId())
        .items(toItems(entity.getItems()))
        .createdAt(
            entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC));
  }

  private List<OrderRequestItem> toItems(List<ClientOrderRequestItemEntity> items) {
    if (items == null) {
      return List.of();
    }
    return items.stream().map(this::toItem).toList();
  }

  private OrderRequestItem toItem(ClientOrderRequestItemEntity item) {
    return new OrderRequestItem()
        .id(item.getId())
        .itemId(item.getItem() == null ? null : item.getItem().getId())
        .sizeId(item.getItemSize() == null ? null : item.getItemSize().getId())
        .itemName(item.getItemName())
        .category(item.getCategory())
        .sizeInInch(item.getSizeInInch())
        .sizeInMm(item.getSizeInMm())
        .plating(item.getPlating())
        .qtyPc(item.getQtyPc())
        .qtyKg(item.getQtyKg())
        .pendingPc(item.getPendingPc());
  }
}

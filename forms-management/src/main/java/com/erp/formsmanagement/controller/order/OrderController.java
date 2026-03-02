package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.OrderOrderManagementApi;
import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.controller.GenericCrudDelegateV2;
import com.erp.controller.GetAllDelegateV1;
import com.erp.controller.GetAllDelegateV2;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderOrderManagementApi {

  private final OrderService orderService;

  private GenericCrudDelegateV2<Long, NewOrder, Order, Long> crud() {
    return new GenericCrudDelegateV2<>(orderService);
  }

  private GetAllDelegateV2<Long, String, PaginatedResultOrder> page() {
    return new GetAllDelegateV2<>(orderService);
  }

  private GetAllDelegateV1<String, PaginatedPartyOrdersResponse> page2() {
    return new GetAllDelegateV1<>(orderService);
  }

  @Override
  public ResponseEntity<Order> createOrder(Long partyId, NewOrder newOrder) {
    return crud().createOne(partyId, newOrder);
  }

  @Override
  public ResponseEntity<Void> deleteOrder(Long partyId, Long id) {
    return crud().delete(partyId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultOrder> getAllOrders(
      Long partyId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            partyId, GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Order> getOrderById(Long partyId, Long id) {
    return crud().getById(partyId, id);
  }

  @Override
  public ResponseEntity<Order> updateOrder(Long partyId, Long id, NewOrder newOrder) {
    return crud().update(partyId, id, newOrder);
  }

  @Override
  public ResponseEntity<PaginatedPartyOrdersResponse> getAllPartiesWithOrders(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page2()
        .getAll(GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }
}

package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.OrderOrderManagementApi;
import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.controller.GetAllDelegateV1;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController
    extends AbstractCrudControllerV2<Long, NewOrder, Order, String, PaginatedResultOrder>
    implements OrderOrderManagementApi {

  private final GetAllDelegateV1<String, PaginatedPartyOrdersResponse> partyOrdersPage;

  public OrderController(OrderService s) {
    super(s, s);
    this.partyOrdersPage = new GetAllDelegateV1<>(s);
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
    return partyOrdersPage.getAll(
        GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }
}

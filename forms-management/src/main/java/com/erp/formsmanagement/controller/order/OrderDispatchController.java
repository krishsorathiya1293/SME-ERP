package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.OrderDispatchOrderManagementApi;
import com.erp.api.ordermanagement.model.NewOrderDispatch;
import com.erp.api.ordermanagement.model.OrderDispatch;
import com.erp.api.ordermanagement.model.PaginatedResultOrderDispatch;
import com.erp.controller.GenericCrudDelegateV2;
import com.erp.controller.GetAllDelegateV2;
import com.erp.formsmanagement.service.order.OrderDispatchService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderDispatchController implements OrderDispatchOrderManagementApi {

  private final OrderDispatchService orderDispatchService;

  private GenericCrudDelegateV2<Long, NewOrderDispatch, OrderDispatch, Long> crud() {
    return new GenericCrudDelegateV2<>(orderDispatchService);
  }

  private GetAllDelegateV2<Long, String, PaginatedResultOrderDispatch> page() {
    return new GetAllDelegateV2<>(orderDispatchService);
  }

  @Override
  public ResponseEntity<OrderDispatch> createOrderDispatch(
      Long partyId, Long orderId, Long itemId, NewOrderDispatch newOrderDispatch) {
    return crud().createOne(itemId, newOrderDispatch);
  }

  @Override
  public ResponseEntity<Void> deleteOrderDispatch(Long partyId, Long orderId, Long itemId, Long id) {
    return crud().delete(itemId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultOrderDispatch> getAllOrderDispatches(
      Long partyId,
      Long orderId,
      Long itemId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page().getAll(
        itemId, GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<OrderDispatch> getOrderDispatchById(
      Long partyId, Long orderId, Long itemId, Long id) {
    return crud().getById(itemId, id);
  }

  @Override
  public ResponseEntity<OrderDispatch> updateOrderDispatch(
      Long partyId, Long orderId, Long itemId, Long id, NewOrderDispatch newOrderDispatch) {
    return crud().update(itemId, id, newOrderDispatch);
  }
}

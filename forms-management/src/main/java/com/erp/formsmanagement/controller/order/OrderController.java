package com.erp.formsmanagement.controller.order;

import com.erp.api.ordermanagement.OrderOrderManagementApi;
import com.erp.api.ordermanagement.model.MergeOrdersRequest;
import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.api.ordermanagement.model.UpdateOrderScrap;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.controller.GetAllDelegateV1;
import com.erp.formsmanagement.mapper.order.OrderMapper;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController
    extends AbstractCrudControllerV2<Long, NewOrder, Order, String, PaginatedResultOrder>
    implements OrderOrderManagementApi {

  private final GetAllDelegateV1<String, PaginatedPartyOrdersResponse> partyOrdersPage;
  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final OrderService orderService;

  public OrderController(OrderService s, OrderRepository orderRepository, OrderMapper orderMapper) {
    super(s, s);
    this.partyOrdersPage = new GetAllDelegateV1<>(s);
    this.orderRepository = orderRepository;
    this.orderMapper = orderMapper;
    this.orderService = s;
  }

  /**
   * Delete a single order item (not the whole order). Dependent job work / returns / dispatches
   * cascade; if it was the order's last item, the empty order is removed too.
   */
  @DeleteMapping("/api/v1/parties/{partyId}/orders/{orderId}/items/{itemId}")
  public ResponseEntity<Void> deleteOrderItem(
      @PathVariable Long partyId, @PathVariable Long orderId, @PathVariable Long itemId) {
    orderService.deleteItem(partyId, orderId, itemId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Sets the scrap agreed for an order. Keyed on the order alone — the scrap belongs to the order,
   * and the callers that set it (the orders sheet, and approving a client's request) reach it by
   * id without a party in hand.
   */
  @PutMapping("/api/v1/orders/{orderId}/scrap")
  public ResponseEntity<Order> updateOrderScrap(
      @PathVariable Long orderId, @RequestBody UpdateOrderScrap request) {
    return ResponseEntity.ok(orderService.updateScrap(orderId, request.getScrap()));
  }

  /**
   * Folds several of a party's orders into one merged order. See {@link OrderService#mergeOrders}
   * for why a new order is created rather than one of them being grown into.
   */
  @PostMapping("/api/v1/orders/merge")
  public ResponseEntity<Order> mergeOrders(@RequestBody MergeOrdersRequest request) {
    return ResponseEntity.ok(orderService.mergeOrders(request.getOrderIds(), request.getScrap()));
  }

  /** Puts a merged order back into the orders it was made from. */
  @PostMapping("/api/v1/orders/{orderId}/unmerge")
  public ResponseEntity<Void> unmergeOrder(@PathVariable Long orderId) {
    orderService.unmergeOrder(orderId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Global orders listing — flat list across every party. Backs the operations
   * dashboard's Orders KPIs and charts.
   */
  @GetMapping("/api/v1/orders")
  public ResponseEntity<List<Order>> getAllOrdersGlobal(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    int p = page != null ? page : 0;
    int sz = size != null ? size : 500;
    var pageResult = orderRepository.findAll(orderRepository.notMergedAway(), PageRequest.of(p, sz));
    return ResponseEntity.ok(
        pageResult.getContent().stream().map(orderMapper::toDomain).toList());
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

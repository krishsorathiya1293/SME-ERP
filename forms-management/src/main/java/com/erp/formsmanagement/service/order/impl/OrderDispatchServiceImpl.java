package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.NewOrderDispatch;
import com.erp.api.ordermanagement.model.OrderDispatch;
import com.erp.api.ordermanagement.model.PaginatedResultOrderDispatch;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.order.OrderDispatchRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemRepository;
import com.erp.formsmanagement.mapper.order.OrderDispatchMapper;
import com.erp.formsmanagement.service.order.OrderDispatchService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderDispatchServiceImpl
    extends AbstractSpecificationServiceV2<
        OrderDispatchEntity, NewOrderDispatch, OrderDispatch, Long>
    implements OrderDispatchService {

  private final OrderDispatchRepository orderDispatchRepository;
  private final OrderItemRepository orderItemRepository;
  private final ClientOrderFulfillmentService clientOrderFulfillmentService;

  public OrderDispatchServiceImpl(
      OrderDispatchRepository orderDispatchRepository,
      OrderItemRepository orderItemRepository,
      ClientOrderFulfillmentService clientOrderFulfillmentService,
      OrderDispatchMapper orderDispatchMapper) {
    super(orderDispatchRepository, orderDispatchMapper);
    this.orderDispatchRepository = orderDispatchRepository;
    this.orderItemRepository = orderItemRepository;
    this.clientOrderFulfillmentService = clientOrderFulfillmentService;
  }

  @Override
  protected void afterCreate(OrderDispatchEntity entity, Long itemId, NewOrderDispatch request) {
    OrderItemEntity orderItem = orderItemRepository.findById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

    double newPcs = request.getDispatchPcs() != null ? request.getDispatchPcs() : 0;
    double alreadyDispatched = orderDispatchRepository.sumDispatchPcsByOrderItemId(itemId);
    double orderQty = orderItem.getQtyPc() != null ? orderItem.getQtyPc() : 0;

    if (alreadyDispatched + newPcs > orderQty) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format("Cannot dispatch %.0f pc — only %.0f pc remaining (order: %.0f, already dispatched: %.0f)",
              newPcs, orderQty - alreadyDispatched, orderQty, alreadyDispatched));
    }

    entity.setOrderItem(orderItem);
    orderItem.setPendingPc(orderQty - (alreadyDispatched + newPcs));
    orderItemRepository.save(orderItem);
    clientOrderFulfillmentService.syncByOrderItem(orderItem);
  }

  @Override
  protected void afterUpdate(OrderDispatchEntity entity, Long itemId, NewOrderDispatch request) {
    OrderItemEntity orderItem = orderItemRepository.findById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

    double updatedPcs = request.getDispatchPcs() != null ? request.getDispatchPcs() : 0;
    double alreadyDispatched = orderDispatchRepository.sumDispatchPcsByOrderItemId(itemId)
        - (entity.getDispatchPcs() != null ? entity.getDispatchPcs() : 0);
    double orderQty = orderItem.getQtyPc() != null ? orderItem.getQtyPc() : 0;

    if (alreadyDispatched + updatedPcs > orderQty) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format("Cannot dispatch %.0f pc — only %.0f pc remaining (order: %.0f, already dispatched: %.0f)",
              updatedPcs, orderQty - alreadyDispatched, orderQty, alreadyDispatched));
    }

    entity.setOrderItem(orderItem);
    orderItem.setPendingPc(orderQty - (alreadyDispatched + updatedPcs));
    orderItemRepository.save(orderItem);
    clientOrderFulfillmentService.syncByOrderItem(orderItem);
  }

  @Override
  public void deleteById(Long itemId, Long dispatchId) {
    OrderDispatchEntity dispatch = orderDispatchRepository.findById(dispatchId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispatch not found"));

    double deletedPcs = dispatch.getDispatchPcs() != null ? dispatch.getDispatchPcs() : 0;
    OrderItemEntity orderItem = dispatch.getOrderItem();
    orderDispatchRepository.deleteById(dispatchId);

    double remaining = orderDispatchRepository.sumDispatchPcsByOrderItemId(orderItem.getId());
    double orderQty = orderItem.getQtyPc() != null ? orderItem.getQtyPc() : 0;
    orderItem.setPendingPc(orderQty - remaining);
    orderItemRepository.save(orderItem);
    clientOrderFulfillmentService.syncByOrderItem(orderItem);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultOrderDispatch getAll(Long itemId, GetAllQuery<String> query) {
    Specification<OrderDispatchEntity> spec =
        Specification.where(
            (root, q, cb) -> cb.equal(root.get("orderItem").get("id"), itemId));

    Page<OrderDispatchEntity> results =
        orderDispatchRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results, mapper()::toDomain, PaginatedResultOrderDispatch::new, PaginatedResultOrderDispatch::setData);
  }
}

package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.NewOrderDispatch;
import com.erp.api.ordermanagement.model.OrderDispatch;
import com.erp.api.ordermanagement.model.PaginatedResultOrderDispatch;
import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.order.OrderDispatchRepository;
import com.erp.formsmanagement.mapper.order.OrderDispatchMapper;
import com.erp.formsmanagement.service.order.OrderDispatchService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderDispatchServiceImpl
    extends AbstractSpecificationServiceV2<
        OrderDispatchEntity, NewOrderDispatch, OrderDispatch, Long>
    implements OrderDispatchService {

  private final OrderDispatchRepository orderDispatchRepository;

  public OrderDispatchServiceImpl(
      OrderDispatchRepository orderDispatchRepository, OrderDispatchMapper orderDispatchMapper) {
    super(orderDispatchRepository, orderDispatchMapper);
    this.orderDispatchRepository = orderDispatchRepository;
  }

  @Override
  protected void afterCreate(OrderDispatchEntity entity, Long itemId, NewOrderDispatch request) {
    OrderItemEntity orderItem = new OrderItemEntity();
    orderItem.setId(itemId);
    entity.setOrderItem(orderItem);
  }

  @Override
  protected void afterUpdate(OrderDispatchEntity entity, Long itemId, NewOrderDispatch request) {
    afterCreate(entity, itemId, request);
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

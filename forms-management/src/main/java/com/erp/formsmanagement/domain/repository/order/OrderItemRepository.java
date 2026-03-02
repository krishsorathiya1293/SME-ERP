package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.repository.CoreRepository;
import java.util.List;

public interface OrderItemRepository extends CoreRepository<OrderItemEntity, Long> {

  List<OrderItemEntity> findAllByOrderId(Long orderId);
}

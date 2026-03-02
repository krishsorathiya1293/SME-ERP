package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.repository.CoreRepository;
import java.util.List;

public interface OrderDispatchRepository extends CoreRepository<OrderDispatchEntity, Long> {

  List<OrderDispatchEntity> findAllByOrderItemId(Long orderItemId);
}

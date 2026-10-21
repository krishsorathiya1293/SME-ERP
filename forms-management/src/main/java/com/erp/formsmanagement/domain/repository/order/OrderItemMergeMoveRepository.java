package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderItemMergeMoveEntity;
import com.erp.repository.CoreRepository;
import java.util.List;

public interface OrderItemMergeMoveRepository
    extends CoreRepository<OrderItemMergeMoveEntity, Long> {

  List<OrderItemMergeMoveEntity> findAllByMergedItem_IdOrderByIdAsc(Long mergedItemId);
}

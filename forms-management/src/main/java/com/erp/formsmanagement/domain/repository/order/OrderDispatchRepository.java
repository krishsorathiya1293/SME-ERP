package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDispatchRepository extends CoreRepository<OrderDispatchEntity, Long> {

  List<OrderDispatchEntity> findAllByOrderItemId(Long orderItemId);

  @Query("SELECT COALESCE(SUM(d.dispatchPcs), 0) FROM OrderDispatchEntity d WHERE d.orderItem.id = :itemId")
  Double sumDispatchPcsByOrderItemId(@Param("itemId") Long itemId);
}

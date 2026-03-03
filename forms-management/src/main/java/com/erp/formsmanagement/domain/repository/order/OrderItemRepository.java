package com.erp.formsmanagement.domain.repository.order;

import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends CoreRepository<OrderItemEntity, Long> {

  List<OrderItemEntity> findAllByOrderId(Long orderId);

  @Query(
"""
    SELECT COALESCE(MAX(oi.jobWorkNo), 0)
    FROM OrderItemEntity oi
    WHERE oi.createdAt >= :startDate
      AND oi.createdAt < :endDate
""")
  Double findMaxJobWorkNoForMonth(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

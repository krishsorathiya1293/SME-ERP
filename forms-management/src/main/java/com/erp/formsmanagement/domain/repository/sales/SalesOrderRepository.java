package com.erp.formsmanagement.domain.repository.sales;

import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderRepository extends CoreRepository<SalesOrderEntity, Long> {
  List<SalesOrderEntity> findByPartyIdAndOrderDateBetweenOrderByOrderDateAsc(
      Long partyId, LocalDate startDate, LocalDate endDate);
}

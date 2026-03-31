package com.erp.formsmanagement.domain.repository.purchase;

import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends CoreRepository<PurchaseOrderEntity, Long> {
  List<PurchaseOrderEntity> findByPartyIdAndOrderDateBetweenOrderByOrderDateAsc(
      Long partyId, LocalDate startDate, LocalDate endDate);
}

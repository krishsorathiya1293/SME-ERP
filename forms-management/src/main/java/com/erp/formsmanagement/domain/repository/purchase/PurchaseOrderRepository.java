package com.erp.formsmanagement.domain.repository.purchase;

import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends CoreRepository<PurchaseOrderEntity, Long> {}

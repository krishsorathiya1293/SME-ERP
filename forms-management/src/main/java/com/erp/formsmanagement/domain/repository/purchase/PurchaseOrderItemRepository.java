package com.erp.formsmanagement.domain.repository.purchase;

import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderItemEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderItemRepository extends CoreRepository<PurchaseOrderItemEntity, Long> {}

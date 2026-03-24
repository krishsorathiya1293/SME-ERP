package com.erp.formsmanagement.domain.repository.sales;

import com.erp.formsmanagement.domain.entity.sales.SalesOrderItemEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderItemRepository extends CoreRepository<SalesOrderItemEntity, Long> {}

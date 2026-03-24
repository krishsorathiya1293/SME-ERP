package com.erp.formsmanagement.domain.repository.sales;

import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderRepository extends CoreRepository<SalesOrderEntity, Long> {}

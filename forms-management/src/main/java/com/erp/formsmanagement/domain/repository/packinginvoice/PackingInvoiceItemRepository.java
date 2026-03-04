package com.erp.formsmanagement.domain.repository.packinginvoice;

import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackingInvoiceItemRepository
    extends JpaRepository<PackingInvoiceItemEntity, Long> {}

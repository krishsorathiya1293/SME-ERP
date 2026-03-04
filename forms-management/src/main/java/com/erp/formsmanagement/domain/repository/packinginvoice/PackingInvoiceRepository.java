package com.erp.formsmanagement.domain.repository.packinginvoice;

import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PackingInvoiceRepository
    extends JpaRepository<PackingInvoiceEntity, Long>,
        JpaSpecificationExecutor<PackingInvoiceEntity> {

  /** Used to auto-generate invoiceNo: count invoices for the same party+date. */
  long countByInvoiceDateAndParty_Id(LocalDate invoiceDate, Long partyId);

  /** Used to auto-generate cartoonNo: cumulative count of all invoices for a party. */
  long countByParty_Id(Long partyId);
}

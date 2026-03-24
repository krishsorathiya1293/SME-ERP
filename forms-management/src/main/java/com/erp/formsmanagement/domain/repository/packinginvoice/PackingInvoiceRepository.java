package com.erp.formsmanagement.domain.repository.packinginvoice;

import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceEntity;
import java.time.LocalDate;
import com.erp.repository.CoreRepository;

public interface PackingInvoiceRepository extends CoreRepository<PackingInvoiceEntity, Long> {

  /** Used to auto-generate invoiceNo: count invoices for the same party+date. */
  long countByInvoiceDateAndParty_Id(LocalDate invoiceDate, Long partyId);

  /** Used to auto-generate cartoonNo: cumulative count of all invoices for a party. */
  long countByParty_Id(Long partyId);
}

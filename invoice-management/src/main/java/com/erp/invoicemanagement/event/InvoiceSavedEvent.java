package com.erp.invoicemanagement.event;

import com.erp.invoicemanagement.domain.InvoiceEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvoiceSavedEvent {
  private final InvoiceEntity invoice;
}

package com.erp.formsmanagement.service.invoice;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.service.AbstractDocumentProvider;
import com.erp.service.DocumentDataProvider.DocumentData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceDocumentProvider extends AbstractDocumentProvider<InvoiceType> {

  private final InvoiceService invoiceService;

  @Override
  protected Class<InvoiceType> variantType() {
    return InvoiceType.class;
  }

  @Override
  public String formType() {
    return "invoice";
  }

  @Override
  public DocumentData resolve(Long id, String variant) {
    InvoiceType type = toVariant(variant);
    Invoice invoice = invoiceService.getInvoiceByType(id, variant);
    return new DocumentData(
        resolveTemplateName(type),
        Map.of(
            "invoice", invoice,
            "currencyType", invoice.getItems().getFirst().getCurrency().name(),
            "todayDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))));
  }

  private String resolveTemplateName(InvoiceType invoiceType) {
    return switch (invoiceType) {
      case EXPORT -> "invoice-export";
      case COMMERCIAL -> "invoice-commercial";
      case PACKAGING_LIST -> "invoice-packaging-list";
    };
  }
}

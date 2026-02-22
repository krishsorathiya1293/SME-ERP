package com.erp.exportmanagement.service.impl;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.exportmanagement.PdfService;
import com.erp.exportmanagement.service.ExportService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {
  private final PdfService pdfService;

  @Override
  public ByteArrayResource generateInvoicePdf(Invoice invoice) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("invoice", invoice);
    variables.put("currencyType", invoice.getItems().getFirst().getCurrency().name());
    variables.put("todayDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
    return pdfService.generatePdf(resolveTemplateName(invoice.getInvoiceType()), variables);
  }

  @Override
  public String shortName(String fullName) {
    return Optional.ofNullable(fullName)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.split("\\s+"))
        .stream()
        .flatMap(Arrays::stream)
        .filter(part -> !part.isEmpty())
        .map(part -> String.valueOf(Character.toUpperCase(part.charAt(0))))
        .collect(Collectors.joining());
  }

  private String resolveTemplateName(InvoiceType invoiceType) {
    return switch (invoiceType) {
      case EXPORT -> "invoice-export";
      case COMMERCIAL -> "invoice-commercial";
      case PACKAGING_LIST -> "invoice-packaging-list";
    };
  }
}

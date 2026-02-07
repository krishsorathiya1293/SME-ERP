package com.erp.invoicemanagement.service;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.exception.PdfGenerationFailedException;
import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {
  private final TemplateEngine templateEngine;

  public byte[] generatePdf(Invoice invoice) {
    Context context = new Context();
    context.setVariable("invoice", invoice);
    String templateName = resolveTemplateName(invoice.getInvoiceType());
    String htmlContent = templateEngine.process(templateName, context);

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Configure PDF properties for A4 page size
      com.itextpdf.html2pdf.ConverterProperties converterProperties = new com.itextpdf.html2pdf.ConverterProperties();

      HtmlConverter.convertToPdf(htmlContent, outputStream, converterProperties);

      log.info("PDF generated successfully for invoice type: {}", invoice.getInvoiceType());
      byte[] pdfBytes = outputStream.toByteArray();
      log.info("PDF size: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (Exception e) {
      log.error("Failed to generate PDF", e);
      throw new PdfGenerationFailedException("Failed to generate PDF: " + e.getMessage());
    }
  }

  private String resolveTemplateName(InvoiceType invoiceType) {
    return switch (invoiceType) {
      case EXPORT -> "invoice-export";
      case COMMERCIAL -> "invoice-commercial";
      case PACKAGING_LIST -> "invoice-packaging-list";
    };
  }

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
}

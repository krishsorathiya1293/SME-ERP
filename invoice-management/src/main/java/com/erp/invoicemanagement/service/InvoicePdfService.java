package com.erp.invoicemanagement.service;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.exception.PdfGenerationFailedException;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {
  private final TemplateEngine templateEngine;

  public byte[] generatePdf(Invoice invoice) {
    Context context = new Context();
    context.setVariable("invoice", invoice);
    String templateName = resolveTemplateName(invoice.getInvoiceType());
    String htmlContent = templateEngine.process(templateName, context);
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(htmlContent);
      renderer.layout();
      renderer.createPDF(outputStream);
      return outputStream.toByteArray();
    } catch (DocumentException | IOException e) {
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

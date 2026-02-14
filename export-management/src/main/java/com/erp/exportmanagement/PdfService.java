package com.erp.exportmanagement;

import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {
  private final TemplateEngine templateEngine;

  public ByteArrayResource generatePdf(String templateName, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    String htmlContent = templateEngine.process(templateName, context);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      HtmlConverter.convertToPdf(htmlContent, outputStream);
      byte[] pdfBytes = outputStream.toByteArray();
      log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);
      return new ByteArrayResource(pdfBytes);
    } catch (Exception e) {
      log.error("Failed to generate PDF", e);
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }
}

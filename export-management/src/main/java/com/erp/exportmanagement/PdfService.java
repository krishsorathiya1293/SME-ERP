package com.erp.exportmanagement;

import com.erp.exportmanagement.assets.Base64Assets;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
  private final Base64Assets base64Assets;
  private final ConverterProperties converterProperties = createConverterProperties();

  private ConverterProperties createConverterProperties() {
    ConverterProperties props = new ConverterProperties();
    FontProvider fontProvider = new FontProvider();
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/KantumruyPro-Regular.ttf"))
            .toExternalForm());
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/KantumruyPro-Medium.ttf"))
            .toExternalForm());
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/KantumruyPro-Bold.ttf"))
            .toExternalForm());
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/KantumruyPro-Light.ttf"))
            .toExternalForm());
    props.setFontProvider(fontProvider);
    return props;
  }

  public ByteArrayResource generatePdf(String templateName, Map<String, Object> variables) {
    Map<String, Object> vars = new HashMap<>(variables);
    vars.putIfAbsent("companyLogoDataUri", base64Assets.getCompanyLogo());
    vars.putIfAbsent("stampDataUri", base64Assets.getStamp());
    Context context = new Context();
    context.setVariables(variables);
    String htmlContent = templateEngine.process(templateName, context);
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(32 * 1024)) {
      HtmlConverter.convertToPdf(htmlContent, outputStream, converterProperties);
      byte[] pdfBytes = outputStream.toByteArray();
      log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);
      return new ByteArrayResource(pdfBytes);
    } catch (Exception e) {
      log.error("Failed to generate PDF", e);
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }
}

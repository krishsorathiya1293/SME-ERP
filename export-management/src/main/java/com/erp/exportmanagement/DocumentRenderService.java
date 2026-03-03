package com.erp.exportmanagement;

import com.erp.exception.PdfGenerationFailedException;
import com.erp.exportmanagement.assets.Base64Assets;
import com.erp.service.DocumentFormat;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRenderService {
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
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/NotoSans-Regular.ttf"))
            .toExternalForm());
    fontProvider.addFont(
        Objects.requireNonNull(getClass().getResource("/fonts/NotoSans-Bold.ttf"))
            .toExternalForm());
    props.setFontProvider(fontProvider);
    return props;
  }

  public ByteArrayResource renderDocument(String templateName, Map<String, Object> variables, DocumentFormat format) {
    Map<String, Object> vars = new HashMap<>(variables);
    vars.putIfAbsent("companyLogoDataUri", base64Assets.getCompanyLogo());
    vars.putIfAbsent("stampDataUri", base64Assets.getStamp());
    Context context = new Context();
    context.setVariables(vars);
    String htmlContent = templateEngine.process(templateName, context);
    
    try (ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream(32 * 1024)) {
      HtmlConverter.convertToPdf(htmlContent, pdfOutputStream, converterProperties);
      byte[] pdfBytes = pdfOutputStream.toByteArray();
      
      if (format == DocumentFormat.PDF) {
        log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);
        return new ByteArrayResource(pdfBytes);
      }
      
      // format == DocumentFormat.PNG
      try (PDDocument document = Loader.loadPDF(pdfBytes);
           ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream(64 * 1024)) {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage renderedImage = pdfRenderer.renderImageWithDPI(0, 150);
        ImageIO.write(renderedImage, "PNG", pngOutputStream);
        byte[] pngBytes = pngOutputStream.toByteArray();
        log.info("PNG generated successfully. Size: {} bytes", pngBytes.length);
        return new ByteArrayResource(pngBytes);
      }
    } catch (Exception e) {
      log.error("Failed to generate {} document", format, e);
      throw new PdfGenerationFailedException("Failed to generate " + format + " document: " + e.getMessage());
    }
  }
}

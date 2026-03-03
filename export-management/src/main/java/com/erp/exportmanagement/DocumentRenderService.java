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

    addFont(fontProvider, "/fonts/KantumruyPro-Regular.ttf");
    addFont(fontProvider, "/fonts/KantumruyPro-Medium.ttf");
    addFont(fontProvider, "/fonts/KantumruyPro-Bold.ttf");
    addFont(fontProvider, "/fonts/KantumruyPro-Light.ttf");
    addFont(fontProvider, "/fonts/NotoSans-Regular.ttf");
    addFont(fontProvider, "/fonts/NotoSans-Bold.ttf");

    props.setFontProvider(fontProvider);
    return props;
  }

  private void addFont(FontProvider fontProvider, String fontPath) {
    try (java.io.InputStream is = getClass().getResourceAsStream(fontPath)) {
      if (is != null) {
        fontProvider.addFont(is.readAllBytes());
      } else {
        log.warn("Font not found in classpath: {}", fontPath);
      }
    } catch (Exception e) {
      log.error("Failed to load font from {}", fontPath, e);
    }
  }

  public ByteArrayResource renderDocument(
      String templateName, Map<String, Object> variables, DocumentFormat format) {
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
          ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream(256 * 1024)) {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage renderedImage =
            pdfRenderer.renderImageWithDPI(0, 300f, org.apache.pdfbox.rendering.ImageType.ARGB);
        ImageIO.write(renderedImage, "png", pngOutputStream);
        byte[] pngBytes = pngOutputStream.toByteArray();
        log.info("PNG generated successfully. Size: {} bytes", pngBytes.length);
        return new ByteArrayResource(pngBytes);
      }
    } catch (Exception e) {
      log.error("Failed to generate {} document", format, e);
      throw new PdfGenerationFailedException(
          "Failed to generate " + format + " document: " + e.getMessage());
    }
  }
}

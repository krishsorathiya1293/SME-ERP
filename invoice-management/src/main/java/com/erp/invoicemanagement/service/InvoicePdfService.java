package com.erp.invoicemanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {
  private final TemplateEngine templateEngine;

  //  private Playwright playwright;
  //  private Browser browser;

  //  @PostConstruct
  //  public void init() {
  //    log.info("Initializing Playwright browser for PDF generation...");
  //    playwright = Playwright.create();
  //    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  //    log.info("Playwright browser initialized successfully");
  //  }
  //
  //  @PreDestroy
  //  public void cleanup() {
  //    log.info("Closing Playwright browser...");
  //    if (browser != null) {
  //      browser.close();
  //    }
  //    if (playwright != null) {
  //      playwright.close();
  //    }
  //    log.info("Playwright browser closed");
  //  }
  //
  //  public byte[] generatePdf(Invoice invoice) {
  //    Context context = new Context();
  //    context.setVariable("invoice", invoice);
  //    String templateName = resolveTemplateName(invoice.getInvoiceType());
  //    String htmlContent = templateEngine.process(templateName, context);
  //
  //    try (Page page = browser.newPage()) {
  //      page.setContent(
  //          htmlContent,
  //          new Page.SetContentOptions()
  //              .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
  //
  //      return page.pdf(
  //          new Page.PdfOptions()
  //              .setFormat("A4")
  //              .setPrintBackground(true)
  //              .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
  //    } catch (Exception e) {
  //      log.error("Failed to generate PDF", e);
  //      throw new PdfGenerationFailedException("Failed to generate PDF: " + e.getMessage());
  //    }
  //  }
  //
  //  private String resolveTemplateName(InvoiceType invoiceType) {
  //    return switch (invoiceType) {
  //      case EXPORT -> "invoice-export";
  //      case COMMERCIAL -> "invoice-commercial";
  //      case PACKAGING_LIST -> "invoice-packaging-list";
  //    };
  //  }
  //
  //  public String shortName(String fullName) {
  //    return Optional.ofNullable(fullName)
  //        .map(String::trim)
  //        .filter(s -> !s.isEmpty())
  //        .map(s -> s.split("\\s+"))
  //        .stream()
  //        .flatMap(Arrays::stream)
  //        .filter(part -> !part.isEmpty())
  //        .map(part -> String.valueOf(Character.toUpperCase(part.charAt(0))))
  //        .collect(Collectors.joining());
  //  }
}

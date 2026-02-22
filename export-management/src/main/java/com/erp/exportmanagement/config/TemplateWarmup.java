package com.erp.exportmanagement.config;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceItemsInner;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.api.invoicemanagement.model.ItemCurrency;
import com.erp.exportmanagement.assets.Base64Assets;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateWarmup {

  private final TemplateEngine templateEngine;
  private final Base64Assets base64Assets;
  private static final String[] TEMPLATES = {
    "invoice-export", "invoice-commercial", "invoice-packaging-list"
  };

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

  @PostConstruct
  public void warmup() {
    long start = System.currentTimeMillis();

    // ✅ Full dummy invoice with real values
    Invoice invoice = buildDummyInvoice();

    Context context = new Context();
    context.setVariable("invoice", invoice);
    context.setVariable(
        "companyLogoDataUri", "data:image/png;base64," + base64Assets.getCompanyLogo());

    context.setVariable("stampDataUri", "data:image/jpeg;base64," + base64Assets.getStamp());

    context.setVariable("currencyType", "USD");
    context.setVariable("todayDate", LocalDate.now().format(DATE_FMT));

    for (String template : TEMPLATES) {
      try {
        templateEngine.process(template, context);
        log.info("Template cached: {}", template);
      } catch (Exception e) {
        // Don’t crash the server because warmup is optional
        log.warn("Template warmup failed for {}: {}", template, rootMessage(e));
      }
    }

    log.info("Template warmup completed in {} ms", System.currentTimeMillis() - start);
  }

  private Invoice buildDummyInvoice() {
    Invoice invoice = new Invoice();

    // IDs / dates
    invoice.setId("warmup-inv-001");
    invoice.setInvoiceNo("INV-2026-0001");
    invoice.setInvoiceDate(LocalDate.now());
    invoice.setCreatedAt(LocalDate.now());

    // Exporter
    invoice.setExporterCompanyName("ABC EXPORTS PVT LTD");
    invoice.setExporterContactNo("+91-9876543210");
    invoice.setExporterAddress("Plot 42, MIDC Industrial Area, Surat, Gujarat, India");

    // Bill To
    invoice.setBillToCountry("USA");
    invoice.setBillToName("Global Industrial Supplies Inc.");
    invoice.setBillToContactNo("+1-312-555-7812");
    invoice.setBillToAddress("500 W Madison St, Chicago, IL 60661, USA");

    // Ship To
    invoice.setShipToCountry("USA");
    invoice.setShipToName("Global Industrial Supplies Inc. (Warehouse)");
    invoice.setShipToContactNo("+1-312-555-7812");
    invoice.setShipToAddress("Warehouse 7, 2400 S Halsted St, Chicago, IL 60608, USA");

    // Govt / trade fields
    invoice.setGstNo("24ABCDE1234F1Z5");
    invoice.setIecCode("IEC1234567");
    invoice.setPoNo("PO-778899");
    invoice.setIncoterms("FOB");
    invoice.setPaymentTerms("Advance / Net 30");
    invoice.setPreCarriage("By Road");

    invoice.setCountryOfOrigin("India");
    invoice.setCountryOfFinalDestination("USA");
    invoice.setPortOfLoading("Nhava Sheva (JNPT)");
    invoice.setPortOfDischarge("Port of New York");

    // Items (must be >= 1)
    invoice.setItems(buildDummyItems());

    // Invoice type (choose a valid enum from your InvoiceType)
    // If your enum values differ, set the correct one (EX: EXPORT / COMMERCIAL etc.)
    invoice.setInvoiceType(pickInvoiceTypeSafe());

    // Bank details
    invoice.setBeneficiaryName("ABC EXPORTS PVT LTD");
    invoice.setBankName("State Bank of India");
    invoice.setBranch("Surat Main Branch");
    invoice.setAccountNo("123456789012");
    invoice.setSwiftCode("SBININBBXXX");

    // Costs
    invoice.setFreightCost(120.0);
    invoice.setInsuranceCost(45.0);
    invoice.setOtherCost(25.0);

    // Optional refs
    invoice.setArnNo("ARN-IND-2026-123456");
    invoice.setRodtep("RODTEP-01");
    invoice.setRexNo("REX-2026-998877");

    return invoice;
  }

  private List<InvoiceItemsInner> buildDummyItems() {
    List<InvoiceItemsInner> items = new ArrayList<>();

    InvoiceItemsInner i1 =
        new InvoiceItemsInner(
            "01", "Premium Cotton T-Shirts", "61091000", 100, 5.50, pickItemCurrencySafe());
    i1.setId("item-001");
    i1.setPartNo("TSHIRT-1001");

    // Pricing
    i1.setCurrencyCurrentPrice(1.0); // USD->USD
    i1.setInrPrice(0.0); // optional

    // Packing details
    i1.setQtyPerCarton(10);
    i1.setNoOfCartons(10);

    // Weights
    i1.setNetWeightKg(48.0);
    i1.setGrossWeightKg(52.0);

    // Pallets
    i1.setWoodenPallets(1);

    items.add(i1);

    InvoiceItemsInner i2 =
        new InvoiceItemsInner(
            "02", "Men Denim Jeans", "62034290", 50, 12.00, pickItemCurrencySafe());
    i2.setId("item-002");
    i2.setPartNo("JEANS-2002");
    i2.setCurrencyCurrentPrice(1.0);
    i2.setQtyPerCarton(5);
    i2.setNoOfCartons(10);
    i2.setNetWeightKg(60.0);
    i2.setGrossWeightKg(65.0);
    i2.setWoodenPallets(1);

    items.add(i2);

    return items;
  }

  private InvoiceType pickInvoiceTypeSafe() {
    // Avoid startup crash if enum values differ
    // Prefer first enum constant if you’re unsure
    try {
      // Example: if you have EXPORT
      return InvoiceType.valueOf("EXPORT");
    } catch (Exception ignored) {
      // fallback: first constant
      InvoiceType[] values = InvoiceType.values();
      return values.length > 0 ? values[0] : null;
    }
  }

  private ItemCurrency pickItemCurrencySafe() {
    try {
      return ItemCurrency.valueOf("USD");
    } catch (Exception ignored) {
      ItemCurrency[] values = ItemCurrency.values();
      return values.length > 0 ? values[0] : null;
    }
  }

  private String rootMessage(Throwable t) {
    Throwable cur = t;
    while (cur.getCause() != null) cur = cur.getCause();
    return cur.getMessage();
  }
}

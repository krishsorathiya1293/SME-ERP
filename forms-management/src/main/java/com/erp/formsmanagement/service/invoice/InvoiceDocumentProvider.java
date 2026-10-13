package com.erp.formsmanagement.service.invoice;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceItemsInner;
import com.erp.api.invoicemanagement.model.ItemCurrency;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.service.AbstractDocumentProvider;
import com.erp.service.DocumentDataProvider.DocumentData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
  private static final double DEFAULT_IGST_PERCENT = 18;

  @Override
  public DocumentData resolve(Long id, String variant) {
    InvoiceType type = toVariant(variant);
    Invoice invoice = invoiceService.getInvoiceByType(id, variant);
    List<InvoiceItemsInner> items = invoice.getItems() == null ? List.of() : invoice.getItems();

    // Currency and rate are per invoice now; older invoices carried them on each item.
    ItemCurrency currency =
        invoice.getCurrency() != null
            ? invoice.getCurrency()
            : items.stream()
                .map(InvoiceItemsInner::getCurrency)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ItemCurrency.USD);
    double rate =
        positive(invoice.getExchangeRate())
            ? invoice.getExchangeRate()
            : items.stream()
                .map(InvoiceItemsInner::getCurrencyCurrentPrice)
                .filter(InvoiceDocumentProvider::positive)
                .findFirst()
                .orElse(0d);

    double subtotal = items.stream().mapToDouble(i -> qty(i) * num(i.getUnitPriceUsd())).sum();
    double charges =
        num(invoice.getFreightCost()) + num(invoice.getInsuranceCost()) + num(invoice.getOtherCost());
    double totalInr = (subtotal + charges) * rate;
    // IGST is charged in INR on the goods value (the client's sheet: Sub Total INR x rate).
    boolean igst = Boolean.TRUE.equals(invoice.getIgstEnabled());
    double igstPercent =
        positive(invoice.getIgstPercent()) ? invoice.getIgstPercent() : DEFAULT_IGST_PERCENT;
    double igstBaseInr = subtotal * rate;
    double igstInr = igst ? igstBaseInr * igstPercent / 100 : 0;
    int totalCartons = items.stream().mapToInt(i -> intOf(i.getNoOfCartons())).sum();
    int totalPallets = items.stream().mapToInt(i -> intOf(i.getWoodenPallets())).sum();
    long totalUnits = items.stream().mapToLong(InvoiceDocumentProvider::qty).sum();

    LocalDate signDate = invoice.getSignDate() != null ? invoice.getSignDate() : LocalDate.now();

    Map<String, Object> model = new HashMap<>();
    model.put("invoice", invoice);
    model.put("currencyType", currency.name());
    model.put("currencySymbol", currency == ItemCurrency.EURO ? "€" : "$");
    model.put("rate", rate);
    model.put("subtotal", subtotal);
    model.put("charges", charges);
    model.put("grandTotal", subtotal + charges);
    model.put("totalInr", totalInr);
    model.put("igstEnabled", igst);
    model.put("igstPercentText", new java.text.DecimalFormat("0.##").format(igstPercent));
    model.put("igstBaseInr", igstBaseInr);
    model.put("igstInr", igstInr);
    model.put("grandTotalInr", totalInr + igstInr);
    model.put("totalUnits", totalUnits);
    model.put("totalCartons", totalCartons);
    model.put("totalPallets", totalPallets);
    model.put("cartonRange", totalCartons > 0 ? String.format("01 - %02d", totalCartons) : "");
    model.put("lutEnabled", !Boolean.FALSE.equals(invoice.getLutEnabled()));
    model.put("todayDate", signDate.format(DATE));
    model.put(
        "invoiceDateText", invoice.getInvoiceDate() == null ? "" : invoice.getInvoiceDate().format(DATE));
    model.put("poDateText", invoice.getPoDate() == null ? "" : invoice.getPoDate().format(DATE));
    return new DocumentData(resolveTemplateName(type), model);
  }

  private static boolean positive(Double d) {
    return d != null && d > 0;
  }

  private static double num(Double d) {
    return d == null ? 0 : d;
  }

  private static int intOf(Integer i) {
    return i == null ? 0 : i;
  }

  private static long qty(InvoiceItemsInner i) {
    return i.getTotalQuantity() == null ? 0 : i.getTotalQuantity();
  }

  private String resolveTemplateName(InvoiceType invoiceType) {
    return switch (invoiceType) {
      case EXPORT -> "invoice-export";
      case COMMERCIAL -> "invoice-commercial";
      case PACKAGING_LIST -> "invoice-packaging-list";
    };
  }
}

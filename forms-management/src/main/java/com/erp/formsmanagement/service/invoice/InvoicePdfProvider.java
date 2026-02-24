package com.erp.formsmanagement.service.invoice;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceType;
import com.erp.service.PdfDataProvider;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoicePdfProvider implements PdfDataProvider {

    private final InvoiceService invoiceService;

    @Override
    public String formType() {
        return "invoice";
    }

    @Override
    public List<String> variants() {
        return Arrays.stream(InvoiceType.values()).map(Enum::name).toList();
    }

    @Override
    public PdfData resolve(Long id, String variant) {
        Invoice invoice = invoiceService.getInvoiceByType(id, variant);
        return new PdfData(
                resolveTemplateName(InvoiceType.valueOf(variant)),
                Map.of(
                        "invoice", invoice,
                        "currencyType", invoice.getItems().getFirst().getCurrency().name(),
                        "todayDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))));
    }

    private String resolveTemplateName(InvoiceType invoiceType) {
        return switch (invoiceType) {
            case EXPORT -> "invoice-export";
            case COMMERCIAL -> "invoice-commercial";
            case PACKAGING_LIST -> "invoice-packaging-list";
        };
    }
}

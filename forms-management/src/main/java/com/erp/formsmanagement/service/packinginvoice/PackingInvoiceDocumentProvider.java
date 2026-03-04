package com.erp.formsmanagement.service.packinginvoice;

import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceEntity;
import com.erp.formsmanagement.domain.repository.packinginvoice.PackingInvoiceRepository;
import com.erp.service.DocumentDataProvider;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackingInvoiceDocumentProvider implements DocumentDataProvider {

  private final PackingInvoiceRepository packingInvoiceRepository;

  @Override
  public List<String> variants() {
    return Collections.singletonList("ALL");
  }

  @Override
  public String formType() {
    return "packing-invoice-party";
  }

  @Override
  public DocumentData resolve(Long id, String variant) {
    // id represents the invoice ID
    PackingInvoiceEntity invoice =
        packingInvoiceRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("PackingInvoice not found: " + id));

    Map<String, Object> variables = new HashMap<>();
    variables.put("invoice", invoice);
    variables.put("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

    return new DocumentData("packing-invoice-party", variables);
  }
}

package com.erp.service;

import java.util.List;
import java.util.Map;

public interface PdfDataProvider {

    String formType();

    List<String> variants();

    PdfData resolve(Long id, String variant);

    record PdfData(String templateName, Map<String, Object> variables) {
    }
}

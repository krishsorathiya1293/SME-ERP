package com.erp.service;

import java.util.List;
import java.util.Map;

public interface DocumentDataProvider {
  String formType();

  List<String> variants();

  DocumentData resolve(Long id, String variant);

  record DocumentData(String templateName, Map<String, Object> variables) {}
}

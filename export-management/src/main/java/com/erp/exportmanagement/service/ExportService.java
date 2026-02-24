package com.erp.exportmanagement.service;

import org.springframework.core.io.ByteArrayResource;

public interface ExportService {

  /** Get a cached PDF. Generates and caches on first call. */
  byte[] getCachedPdf(String formType, Long id, String variant);

  /** Evict all cached PDFs for a given form type and entity. */
  void evictCache(String formType, Long id);

  /** Generate a PDF without caching. */
  ByteArrayResource generatePdf(String formType, Long id, String variant);

  /** Create a short name from initials, e.g. "Acme Corp" → "AC". */
  String shortName(String fullName);
}

package com.erp.exportmanagement.service;

import com.erp.service.DocumentFormat;
import org.springframework.core.io.ByteArrayResource;

public interface ExportService {

  /** Get a cached document. Generates and caches on first call. */
  byte[] getCachedDocument(String formType, Long id, String variant, DocumentFormat format);

  /** Evict all cached documents for a given form type and entity. */
  void evictCache(String formType, Long id);

  /** Generate a document without caching. */
  ByteArrayResource generateDocument(String formType, Long id, String variant, DocumentFormat format);

  /** Create a short name from initials, e.g. "Acme Corp" → "AC". */
  String shortName(String fullName);
}

package com.erp.exportmanagement.service.impl;

import com.erp.exportmanagement.PdfService;
import com.erp.exportmanagement.service.ExportService;
import com.erp.service.PdfDataProvider;
import com.erp.service.PdfDataProvider.PdfData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExportServiceImpl implements ExportService {

  private final PdfService pdfService;
  private final Map<String, PdfDataProvider> providerRegistry;
  private final org.springframework.cache.CacheManager cacheManager;

  public ExportServiceImpl(
      PdfService pdfService,
      List<PdfDataProvider> providers,
      org.springframework.cache.CacheManager cacheManager) {
    this.pdfService = pdfService;
    this.providerRegistry =
        providers.stream()
            .collect(Collectors.toMap(PdfDataProvider::formType, Function.identity()));
    this.cacheManager = cacheManager;
    log.info("Registered PDF providers: {}", providerRegistry.keySet());
  }

  @Override
  @Cacheable(value = "pdfCache", key = "#formType + '-' + #id + '-' + #variant")
  public byte[] getCachedPdf(String formType, Long id, String variant) {
    log.info("Cache MISS — generating PDF for {} id={} variant={}", formType, id, variant);
    return generatePdf(formType, id, variant).getByteArray();
  }

  @Override
  public ByteArrayResource generatePdf(String formType, Long id, String variant) {
    PdfData data = getProvider(formType).resolve(id, variant);
    return pdfService.generatePdf(data.templateName(), data.variables());
  }

  @Override
  public void evictCache(String formType, Long id) {
    log.info("Evicting PDF cache for {} id={}", formType, id);
    Cache cache = cacheManager.getCache("pdfCache");
    if (cache != null) {
      Object nativeCache = cache.getNativeCache();
      if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine =
            (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;
        String prefix = formType + "-" + id + "-";
        caffeine
            .asMap()
            .keySet()
            .removeIf(
                key -> {
                  if (key instanceof String) {
                    return ((String) key).startsWith(prefix);
                  }
                  return false;
                });
      }
    }
  }

  @Override
  public String shortName(String fullName) {
    return Optional.ofNullable(fullName)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.split("\\s+"))
        .stream()
        .flatMap(Arrays::stream)
        .filter(part -> !part.isEmpty())
        .map(part -> String.valueOf(Character.toUpperCase(part.charAt(0))))
        .collect(Collectors.joining());
  }

  private PdfDataProvider getProvider(String formType) {
    PdfDataProvider provider = providerRegistry.get(formType);
    if (provider == null) {
      throw new IllegalArgumentException(
          "No PdfDataProvider registered for: "
              + formType
              + ". Available: "
              + providerRegistry.keySet());
    }
    return provider;
  }
}

package com.erp.exportmanagement.service.impl;

import com.erp.exportmanagement.DocumentRenderService;
import com.erp.exportmanagement.service.ExportService;
import com.erp.service.DocumentDataProvider;
import com.erp.service.DocumentDataProvider.DocumentData;
import com.erp.service.DocumentFormat;
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

  private final DocumentRenderService documentRenderService;
  private final Map<String, DocumentDataProvider> providerRegistry;
  private final org.springframework.cache.CacheManager cacheManager;

  public ExportServiceImpl(
      DocumentRenderService documentRenderService,
      List<DocumentDataProvider> providers,
      org.springframework.cache.CacheManager cacheManager) {
    this.documentRenderService = documentRenderService;
    this.providerRegistry =
        providers.stream()
            .collect(Collectors.toMap(DocumentDataProvider::formType, Function.identity()));
    this.cacheManager = cacheManager;
    log.info("Registered Document providers: {}", providerRegistry.keySet());
  }

  @Override
  @Cacheable(value = "pdfCache", key = "#formType + '-' + #id + '-' + #variant + '-' + #format.name()")
  public byte[] getCachedDocument(String formType, Long id, String variant, DocumentFormat format) {
    log.info("Cache MISS — generating {} for {} id={} variant={}", format, formType, id, variant);
    return generateDocument(formType, id, variant, format).getByteArray();
  }

  @Override
  public ByteArrayResource generateDocument(String formType, Long id, String variant, DocumentFormat format) {
    DocumentData data = getProvider(formType).resolve(id, variant);
    return documentRenderService.renderDocument(data.templateName(), data.variables(), format);
  }

  @Override
  public void evictCache(String formType, Long id) {
    log.info("Evicting Document cache for {} id={}", formType, id);
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

  private DocumentDataProvider getProvider(String formType) {
    DocumentDataProvider provider = providerRegistry.get(formType);
    if (provider == null) {
      throw new IllegalArgumentException(
          "No DocumentDataProvider registered for: "
              + formType
              + ". Available: "
              + providerRegistry.keySet());
    }
    return provider;
  }
}

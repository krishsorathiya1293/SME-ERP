package com.erp.exportmanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("pdfCache");
    // Short TTL: the cache only needs to de-dupe the burst of repeat fetches for the same document
    // (download + print preview). A long TTL made edits to translations/data lag behind the print
    // for up to 10 minutes; regenerating a small chitthi is cheap, so keep it fresh.
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats());
    return cacheManager;
  }
}

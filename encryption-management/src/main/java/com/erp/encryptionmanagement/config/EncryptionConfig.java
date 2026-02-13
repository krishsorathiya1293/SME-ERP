package com.erp.encryptionmanagement.config;

import com.erp.encryptionmanagement.service.EncryptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

  @Bean
  public EncryptionService encryptionService(EncryptionProperties properties) {
    return new EncryptionService(properties);
  }
}

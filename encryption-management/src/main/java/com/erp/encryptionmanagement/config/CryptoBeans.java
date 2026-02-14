package com.erp.encryptionmanagement.config;

import com.erp.encryptionmanagement.crypto.AesGcmCipher;
import com.erp.encryptionmanagement.crypto.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoBeans {

  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }

  @Bean
  public JsonUtil jsonUtil(ObjectMapper objectMapper) {
    return new JsonUtil(objectMapper);
  }

  @Bean
  public AesGcmCipher aesGcmCipher(ObjectMapper objectMapper) {
    return new AesGcmCipher(objectMapper);
  }
}

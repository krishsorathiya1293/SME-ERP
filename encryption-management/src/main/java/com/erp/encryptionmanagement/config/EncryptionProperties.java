package com.erp.encryptionmanagement.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "encryption")
@Component
public class EncryptionProperties {

  /** Keep this as AES/GCM/NoPadding for maximum security. */
  private String cipherAlgorithm = "AES/GCM/NoPadding";

  /** For AES/GCM this should be "AES". If empty, derived from cipherAlgorithm. */
  private String keyAlgorithm;

  /**
   * Backward compatible single key (base64 of 16/24/32 bytes). Prefer keysBase64 + currentKeyId for
   * rotation.
   */
  private String secretKeyBase64;

  /** Recommended: key rotation map: keyId -> base64(keyBytes). */
  private Map<String, String> keysBase64 = new HashMap<>();

  /** Active key id used for NEW encryptions (required when keysBase64 is used). */
  private String currentKeyId;

  /** Prefix to mark encrypted DB values. */
  private String prefix = "ENC:";

  /** Enable/disable encryption. */
  private boolean enabled = true;

  /** 12 bytes is standard for GCM IV. */
  private int gcmIvLength = 12;

  /** 128-bit tag is standard. */
  private int gcmTagLength = 128;

  /** Hardening: fail fast if cipherAlgorithm is not GCM. */
  private boolean requireGcm = true;

  /**
   * Optional AAD (authenticated but not encrypted). If you change this later, old ciphertext will
   * NOT decrypt.
   */
  private String aad = "";
}

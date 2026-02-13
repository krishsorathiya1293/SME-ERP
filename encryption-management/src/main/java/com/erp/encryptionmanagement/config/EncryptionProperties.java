package com.erp.encryptionmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "encryption")
@Component
public class EncryptionProperties {

  /** Cipher transformation, e.g. "AES/GCM/NoPadding" (recommended) or "AES". */
  private String cipherAlgorithm = "AES/GCM/NoPadding";

  /**
   * Key algorithm for SecretKeySpec. For AES/GCM this should be "AES". If not set, it will be
   * derived from cipherAlgorithm (text before first '/').
   */
  private String keyAlgorithm;

  /** Base64-encoded secret key (16/24/32 bytes after decoding for AES-128/192/256). */
  private String secretKeyBase64;

  /** Prefix used to mark encrypted values in DB (e.g. "ENC:"). */
  private String prefix = "ENC:";

  /** Enables/disables encryption globally (useful for local/dev). */
  private boolean enabled = true;

  /** GCM IV length in bytes. 12 is the NIST-recommended length. */
  private int gcmIvLength = 12;

  /** GCM authentication tag length in bits. 128 is common. */
  private int gcmTagLength = 128;
}

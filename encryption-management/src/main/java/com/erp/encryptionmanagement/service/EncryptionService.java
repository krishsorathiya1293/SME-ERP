package com.erp.encryptionmanagement.service;

import com.erp.encryptionmanagement.config.EncryptionProperties;
import com.erp.exception.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/**
 * Stateless, thread-safe encryption service.
 *
 * <p>Recommended config: - encryption.cipher-algorithm=AES/GCM/NoPadding -
 * encryption.secret-key-base64=... (base64 of 16/24/32 bytes) - encryption.prefix=ENC:
 */
public class EncryptionService {

  private static final String VERSION_V1 = "v1:";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final EncryptionProperties properties;
  private final SecretKeySpec secretKey;

  public EncryptionService(EncryptionProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
    validate(properties);
    this.secretKey = buildSecretKey(properties);
  }

  public boolean isEncrypted(String value) {
    return value != null && value.startsWith(properties.getPrefix());
  }

  public String encryptIfNeeded(String value) {
    if (!properties.isEnabled() || value == null || isEncrypted(value)) {
      return value;
    }
    try {
      return properties.getPrefix() + VERSION_V1 + encryptInternal(value);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Encryption failed" + e.getMessage());
    }
  }

  public String decryptIfNeeded(String value) {
    if (!properties.isEnabled() || value == null || !isEncrypted(value)) {
      return value;
    }
    String payload = value.substring(properties.getPrefix().length());

    // Backward compatibility: if old data doesn't have a version prefix, try legacy decrypt.
    try {
      if (payload.startsWith(VERSION_V1)) {
        return decryptInternal(payload.substring(VERSION_V1.length()));
      }
      return decryptLegacy(payload);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Decryption failed" + e.getMessage());
    }
  }

  private String encryptInternal(String plainText) throws GeneralSecurityException {
    String cipherAlg = properties.getCipherAlgorithm();

    if (isGcm(cipherAlg)) {
      byte[] iv = new byte[properties.getGcmIvLength()];
      SECURE_RANDOM.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(cipherAlg);
      cipher.init(
          Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(properties.getGcmTagLength(), iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      // Store as: base64(iv):base64(cipherText)
      return Base64.getEncoder().encodeToString(iv)
          + ":"
          + Base64.getEncoder().encodeToString(encrypted);
    }

    // Fallback (legacy/non-AEAD). Prefer GCM above.
    Cipher cipher = Cipher.getInstance(cipherAlg);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    return Base64.getEncoder()
        .encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
  }

  private String decryptInternal(String payload) throws GeneralSecurityException {
    String cipherAlg = properties.getCipherAlgorithm();

    if (isGcm(cipherAlg)) {
      String[] parts = payload.split(":", 2);
      if (parts.length != 2) {
        throw new EncryptionException(
            "Invalid encrypted payload format for GCM (expected iv:cipherText)");
      }

      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] cipherText = Base64.getDecoder().decode(parts[1]);

      Cipher cipher = Cipher.getInstance(cipherAlg);
      cipher.init(
          Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(properties.getGcmTagLength(), iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    // Fallback (legacy/non-AEAD)
    Cipher cipher = Cipher.getInstance(cipherAlg);
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    return new String(cipher.doFinal(Base64.getDecoder().decode(payload)), StandardCharsets.UTF_8);
  }

  /**
   * Legacy decrypt: matches the older implementation that stored base64(cipherText) with no
   * IV/version. Uses current cipherAlgorithm configuration.
   */
  private String decryptLegacy(String payload) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    return new String(cipher.doFinal(Base64.getDecoder().decode(payload)), StandardCharsets.UTF_8);
  }

  private static boolean isGcm(String cipherAlg) {
    return cipherAlg != null && cipherAlg.toUpperCase().contains("GCM");
  }

  private static void validate(EncryptionProperties p) {
    if (!StringUtils.hasText(p.getPrefix())) {
      throw new IllegalArgumentException("encryption.prefix must not be blank");
    }
    if (!StringUtils.hasText(p.getCipherAlgorithm())) {
      throw new IllegalArgumentException("encryption.cipher-algorithm must not be blank");
    }
    if (!StringUtils.hasText(p.getSecretKeyBase64())) {
      throw new IllegalArgumentException("encryption.secret-key-base64 must not be blank");
    }
    if (p.getGcmIvLength() <= 0) {
      throw new IllegalArgumentException("encryption.gcm-iv-length must be > 0");
    }
    if (p.getGcmTagLength() <= 0) {
      throw new IllegalArgumentException("encryption.gcm-tag-length must be > 0");
    }
  }

  private static SecretKeySpec buildSecretKey(EncryptionProperties p) {
    byte[] keyBytes = Base64.getDecoder().decode(p.getSecretKeyBase64());
    String keyAlg =
        StringUtils.hasText(p.getKeyAlgorithm())
            ? p.getKeyAlgorithm()
            : deriveKeyAlgorithm(p.getCipherAlgorithm());

    // AES expects 16/24/32 bytes
    if ("AES".equalsIgnoreCase(keyAlg)
        && !(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
      throw new IllegalArgumentException(
          "Invalid AES key length: " + keyBytes.length + " bytes. Expected 16/24/32 bytes.");
    }
    return new SecretKeySpec(keyBytes, keyAlg);
  }

  private static String deriveKeyAlgorithm(String cipherAlgorithm) {
    int slash = cipherAlgorithm.indexOf('/');
    return (slash > 0) ? cipherAlgorithm.substring(0, slash) : cipherAlgorithm;
  }
}

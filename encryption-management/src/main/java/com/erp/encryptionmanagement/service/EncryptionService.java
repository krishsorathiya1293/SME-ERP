package com.erp.encryptionmanagement.service;

import com.erp.encryptionmanagement.config.EncryptionProperties;
import com.erp.exception.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/**
 * Stateless, thread-safe encryption service.
 *
 * <p>Ciphertext formats: - v2 (recommended): <prefix>v2:<keyId>:<b64url(iv)>:<b64url(cipherText)> -
 * v1 (backward compat): <prefix>v1:<base64(iv)>:<base64(cipherText)> - legacy (migration only):
 * <prefix><base64(cipherText)> (no IV/version)
 */
public class EncryptionService {

  private static final String VERSION_V2 = "v2:";
  private static final String VERSION_V1 = "v1:";

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final EncryptionProperties properties;

  private final Base64.Encoder b64urlEnc = Base64.getUrlEncoder().withoutPadding();
  private final Base64.Decoder b64urlDec = Base64.getUrlDecoder();

  /** keyId -> key */
  private final Map<String, SecretKeySpec> keysById;

  private final String currentKeyId;

  public EncryptionService(EncryptionProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
    validate(properties);
    this.keysById = buildKeys(properties);
    this.currentKeyId = resolveCurrentKeyId(properties, keysById);
  }

  public boolean isEncrypted(String value) {
    return value != null && value.startsWith(properties.getPrefix());
  }

  public String encryptIfNeeded(String value) {
    if (!properties.isEnabled() || value == null || isEncrypted(value)) return value;

    try {
      return properties.getPrefix() + encryptV2(value);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Encryption failed: " + e.getMessage());
    }
  }

  public String decryptIfNeeded(String value) {
    if (!properties.isEnabled() || value == null || !isEncrypted(value)) return value;

    String payload = value.substring(properties.getPrefix().length());
    try {
      if (payload.startsWith(VERSION_V2)) return decryptV2(payload.substring(VERSION_V2.length()));
      if (payload.startsWith(VERSION_V1)) return decryptV1(payload.substring(VERSION_V1.length()));
      return decryptLegacy(payload); // migration only
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Decryption failed: " + e.getMessage());
    }
  }

  private String encryptV2(String plainText) throws GeneralSecurityException {
    ensureGcm();

    byte[] iv = new byte[properties.getGcmIvLength()];
    SECURE_RANDOM.nextBytes(iv);

    SecretKeySpec key = keysById.get(currentKeyId);
    Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(properties.getGcmTagLength(), iv));
    applyAad(cipher);

    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

    return VERSION_V2
        + currentKeyId
        + ":"
        + b64urlEnc.encodeToString(iv)
        + ":"
        + b64urlEnc.encodeToString(cipherText);
  }

  private String decryptV2(String payload) throws GeneralSecurityException {
    ensureGcm();

    // keyId:iv:cipherText
    String[] parts = payload.split(":", 3);
    if (parts.length != 3) {
      throw new EncryptionException("Invalid v2 payload (expected keyId:iv:cipherText)");
    }

    String keyId = parts[0];
    SecretKeySpec key = keysById.get(keyId);
    if (key == null) throw new EncryptionException("No key configured for keyId: " + keyId);

    byte[] iv = b64urlDec.decode(parts[1]);
    byte[] cipherText = b64urlDec.decode(parts[2]);

    Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(properties.getGcmTagLength(), iv));
    applyAad(cipher);

    return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
  }

  /** Backward compatibility for your current v1 (iv:cipherText, no keyId). */
  private String decryptV1(String payload) throws GeneralSecurityException {
    ensureGcm();

    String[] parts = payload.split(":", 2);
    if (parts.length != 2) {
      throw new EncryptionException("Invalid v1 payload (expected iv:cipherText)");
    }

    // v1 didn't store keyId. If you rotate keys, keep the OLD key as current until data is
    // migrated.
    SecretKeySpec key = keysById.get(currentKeyId);

    byte[] iv = Base64.getDecoder().decode(parts[0]);
    byte[] cipherText = Base64.getDecoder().decode(parts[1]);

    Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(properties.getGcmTagLength(), iv));
    applyAad(cipher);

    return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
  }

  /**
   * Migration-only legacy support (no IV/version). Once all rows are re-encrypted to v2, DELETE
   * this method.
   */
  private String decryptLegacy(String payload) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
    SecretKeySpec key = keysById.get(currentKeyId);
    cipher.init(Cipher.DECRYPT_MODE, key);
    return new String(cipher.doFinal(Base64.getDecoder().decode(payload)), StandardCharsets.UTF_8);
  }

  private void applyAad(Cipher cipher) {
    if (StringUtils.hasText(properties.getAad())) {
      cipher.updateAAD(properties.getAad().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void ensureGcm() {
    if (properties.isRequireGcm() && !isGcm(properties.getCipherAlgorithm())) {
      throw new EncryptionException("Non-GCM cipherAlgorithm blocked (require-gcm=true)");
    }
  }

  private static boolean isGcm(String cipherAlg) {
    return cipherAlg != null && cipherAlg.toUpperCase().contains("GCM");
  }

  private static void validate(EncryptionProperties p) {
    if (!StringUtils.hasText(p.getPrefix()))
      throw new IllegalArgumentException("encryption.prefix must not be blank");
    if (!StringUtils.hasText(p.getCipherAlgorithm()))
      throw new IllegalArgumentException("encryption.cipher-algorithm must not be blank");
    if (p.getGcmIvLength() <= 0)
      throw new IllegalArgumentException("encryption.gcm-iv-length must be > 0");
    if (p.getGcmTagLength() <= 0)
      throw new IllegalArgumentException("encryption.gcm-tag-length must be > 0");

    boolean hasRotationKeys = p.getKeysBase64() != null && !p.getKeysBase64().isEmpty();
    boolean hasSingleKey = StringUtils.hasText(p.getSecretKeyBase64());

    if (!hasRotationKeys && !hasSingleKey) {
      throw new IllegalArgumentException(
          "Provide encryption.keys-base64 (recommended) or encryption.secret-key-base64");
    }

    if (hasRotationKeys && !StringUtils.hasText(p.getCurrentKeyId())) {
      throw new IllegalArgumentException(
          "encryption.current-key-id must be set when keys-base64 is used");
    }

    if (p.isRequireGcm() && !isGcm(p.getCipherAlgorithm())) {
      throw new IllegalArgumentException("Use AES/GCM/NoPadding when require-gcm=true");
    }
  }

  private static Map<String, SecretKeySpec> buildKeys(EncryptionProperties p) {
    Map<String, SecretKeySpec> out = new HashMap<>();

    String keyAlg =
        StringUtils.hasText(p.getKeyAlgorithm())
            ? p.getKeyAlgorithm()
            : deriveKeyAlgorithm(p.getCipherAlgorithm());

    if (p.getKeysBase64() != null && !p.getKeysBase64().isEmpty()) {
      for (Map.Entry<String, String> e : p.getKeysBase64().entrySet()) {
        if (!StringUtils.hasText(e.getKey()))
          throw new IllegalArgumentException("Blank keyId in keys-base64");
        if (!StringUtils.hasText(e.getValue()))
          throw new IllegalArgumentException("Blank key material for keyId: " + e.getKey());
        out.put(e.getKey(), toSecretKeySpec(e.getValue(), keyAlg));
      }
      return out;
    }

    // Backward-compatible single key config
    out.put("default", toSecretKeySpec(p.getSecretKeyBase64(), keyAlg));
    return out;
  }

  private static String resolveCurrentKeyId(
      EncryptionProperties p, Map<String, SecretKeySpec> keys) {
    if (p.getKeysBase64() != null && !p.getKeysBase64().isEmpty()) {
      String id = p.getCurrentKeyId();
      if (!keys.containsKey(id)) {
        throw new IllegalArgumentException("current-key-id not found in keys-base64: " + id);
      }
      return id;
    }
    return "default";
  }

  private static SecretKeySpec toSecretKeySpec(String base64Key, String keyAlg) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);

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

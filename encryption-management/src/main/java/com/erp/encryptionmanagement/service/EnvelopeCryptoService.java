package com.erp.encryptionmanagement.service;

import com.erp.encryptionmanagement.crypto.Aad;
import com.erp.encryptionmanagement.crypto.AesGcmCipher;
import com.erp.encryptionmanagement.crypto.GcmBlob;
import com.erp.encryptionmanagement.crypto.JsonUtil;
import com.erp.encryptionmanagement.crypto.payload.EnvelopePayloadV1;
import com.erp.encryptionmanagement.keys.MasterKeyService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Envelope encryption: - dataKey (random per value) encrypts plaintext - masterKey wraps dataKey -
 * payload stores mkv + wrapped dk + encrypted data
 */
@Service
@RequiredArgsConstructor
public class EnvelopeCryptoService {

  private final MasterKeyService masterKeyService;
  private final AesGcmCipher cipher;
  private final JsonUtil json;
  private final SecureRandom rng;

  private static final int DATA_KEY_BYTES = 32;

  public String encrypt(String plaintext) {
    return encrypt(plaintext, null);
  }

  public String encrypt(String plaintext, String context) {
    if (plaintext == null) return null;

    masterKeyService.ensureActive();
    Map<String, String> aad = Aad.fromContext(context);

    // 1) data key per value
    byte[] dataKey = new byte[DATA_KEY_BYTES];
    rng.nextBytes(dataKey);

    // 2) encrypt data
    byte[] dataIv = new byte[AesGcmCipher.ivLengthBytes()];
    rng.nextBytes(dataIv);
    GcmBlob dataBlob =
        cipher.encrypt(dataKey, plaintext.getBytes(StandardCharsets.UTF_8), dataIv, aad);

    // 3) wrap data key with active master key
    byte[] masterKey = masterKeyService.getActiveMasterKey();
    String mkv = masterKeyService.getActiveMasterKeyVersion();

    byte[] dkIv = new byte[AesGcmCipher.ivLengthBytes()];
    rng.nextBytes(dkIv);
    GcmBlob wrappedDk = cipher.encrypt(masterKey, dataKey, dkIv, aad);

    EnvelopePayloadV1 payload =
        new EnvelopePayloadV1(
            EnvelopePayloadV1.VERSION,
            mkv,
            wrappedDk.toBase64Map(),
            dataBlob.toBase64Map(),
            (context == null || context.isBlank()) ? null : context);

    return json.write(payload);
  }

  public String decrypt(String payloadJson) {
    return decrypt(payloadJson, null);
  }

  @SuppressWarnings("unchecked")
  public String decrypt(String payloadJson, String context) {
    if (payloadJson == null) return null;

    // Fast non-JSON check (plaintext row)
    String s = payloadJson.trim();
    if (!s.startsWith("{")
        || !s.contains("\"mkv\"")
        || !s.contains("\"dk\"")
        || !s.contains("\"d\"")) {
      return payloadJson; // plaintext in DB
    }

    Map<String, Object> root = json.readMap(payloadJson);

    Object mkvObj = root.get("mkv");
    Object dkObj = root.get("dk");
    Object dObj = root.get("d");

    if (!(mkvObj instanceof String mkv) || !(dkObj instanceof Map) || !(dObj instanceof Map)) {
      throw new IllegalArgumentException("Invalid envelope payload");
    }

    Map<String, String> aad = Aad.fromContext(context);

    byte[] masterKey = masterKeyService.getMasterKeyForVersion(mkv);

    byte[] dataKey =
        cipher.decrypt(masterKey, GcmBlob.fromBase64Map((Map<String, Object>) dkObj), aad);

    byte[] pt = cipher.decrypt(dataKey, GcmBlob.fromBase64Map((Map<String, Object>) dObj), aad);

    return new String(pt, StandardCharsets.UTF_8);
  }

  /** Master key rotation */
  public String rotateKey() {
    return masterKeyService.rotateMasterKey();
  }
}

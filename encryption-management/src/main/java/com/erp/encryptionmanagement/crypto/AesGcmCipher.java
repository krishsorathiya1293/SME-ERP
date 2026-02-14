package com.erp.encryptionmanagement.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmCipher {

  private static final String TRANSFORM = "AES/GCM/NoPadding";
  private static final String ALG = "AES";
  private static final int IV_LEN = 12;
  private static final int TAG_BITS = 128;

  private final ObjectMapper mapper;

  public AesGcmCipher(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public GcmBlob encrypt(byte[] key, byte[] plaintext, byte[] iv, Map<String, String> aad) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(
          Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_BITS, iv));
      if (aad != null && !aad.isEmpty()) {
        cipher.updateAAD(mapper.writeValueAsBytes(aad));
      }
      byte[] ct = cipher.doFinal(plaintext);
      return new GcmBlob(iv, ct);
    } catch (Exception e) {
      throw new IllegalStateException("AES-GCM encrypt failed", e);
    }
  }

  public byte[] decrypt(byte[] key, GcmBlob blob, Map<String, String> aad) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, ALG),
          new GCMParameterSpec(TAG_BITS, blob.iv()));
      if (aad != null && !aad.isEmpty()) {
        cipher.updateAAD(mapper.writeValueAsBytes(aad));
      }
      return cipher.doFinal(blob.ct());
    } catch (Exception e) {
      throw new IllegalStateException("AES-GCM decrypt failed", e);
    }
  }

  public static int ivLengthBytes() {
    return IV_LEN;
  }

  public static String b64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static byte[] b64d(String s) {
    return Base64.getDecoder().decode(s);
  }
}

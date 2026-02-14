package com.erp.encryptionmanagement.crypto;

import java.util.Map;

public record GcmBlob(byte[] iv, byte[] ct) {

  public Map<String, String> toBase64Map() {
    return Map.of(
        "iv", AesGcmCipher.b64(iv),
        "ct", AesGcmCipher.b64(ct));
  }

  public static GcmBlob fromBase64Map(Map<String, ?> m) {
    if (m == null) throw new IllegalArgumentException("Missing GCM blob");
    String iv = (String) m.get("iv");
    String ct = (String) m.get("ct");
    if (iv == null || ct == null) throw new IllegalArgumentException("Missing iv/ct");
    return new GcmBlob(AesGcmCipher.b64d(iv), AesGcmCipher.b64d(ct));
  }
}

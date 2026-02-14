package com.erp.encryptionmanagement.crypto.payload;

import java.util.Map;

/**
 * Envelope payload v1: - mkv: master key version/alias - dk: wrapped data key (AES-GCM iv/ct) - d :
 * encrypted data (AES-GCM iv/ct) - ctx: optional context (if you choose to store it)
 */
public record EnvelopePayloadV1(
    int v, String mkv, Map<String, String> dk, Map<String, String> d, String ctx) {
  public static final int VERSION = 1;
}

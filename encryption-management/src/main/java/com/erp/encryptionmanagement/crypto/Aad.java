package com.erp.encryptionmanagement.crypto;

import java.util.Map;

public final class Aad {
  private Aad() {}

  /** Bind ciphertext to a context (optional). */
  public static Map<String, String> fromContext(String context) {
    if (context == null || context.isBlank()) return Map.of();
    return Map.of("ctx", context);
  }
}

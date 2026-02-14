package com.erp.encryptionmanagement.keys;

public interface MasterKeyService {
  void ensureActive();

  byte[] getActiveMasterKey();

  String getActiveMasterKeyVersion();

  byte[] getMasterKeyForVersion(String version);

  String rotateMasterKey();
}

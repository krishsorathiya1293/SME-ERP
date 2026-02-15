package com.erp.encryptionmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "master-key-store")
@Component
public class MasterKeyStoreProperties {

  /** /opt/app/keys/master.p12 */
  private String keystorePath;

  /** PKCS12 recommended */
  private String keystoreType = "PKCS12";

  /** Optional: set directly (not recommended for prod) */
  private String keystorePassword;

  /** Recommended: env var name for keystore password */
  private String keystorePasswordEnv = "MASTER_KEYSTORE_PASSWORD";

  /** Where we store active alias, e.g. /opt/app/keys/active.alias */
  private String activeAliasFile;

  /** master-v prefix -> master-v1, master-v2 */
  private String aliasPrefix = "master-v";

  /** Create keystore + initial key if missing */
  private boolean createIfMissing = true;

  /** 32 bytes = AES-256 */
  private int masterKeySizeBytes = 32;

  private String envMasterKeyBase64;

  /** Env var name for base64 master key */
  private String envMasterKeyBase64Env = "MASTER_KEY_BASE64";

  /**
   * The "version" string stored in payloads as mkv when envMasterKeyBase64 is used. Keep stable
   * forever once data is encrypted.
   */
  private String envMasterKeyVersion = "master-v1";
}

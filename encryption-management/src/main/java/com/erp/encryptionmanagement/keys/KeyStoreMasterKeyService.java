package com.erp.encryptionmanagement.keys;

import com.erp.encryptionmanagement.config.MasterKeyStoreProperties;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeyStoreMasterKeyService implements MasterKeyService {

  private final MasterKeyStoreProperties props;
  private final SecureRandom rng;

  private final AtomicReference<byte[]> cachedKey = new AtomicReference<>();
  private final AtomicReference<String> cachedAlias = new AtomicReference<>();

  @Override
  public synchronized void ensureActive() {
    if (cachedKey.get() != null && cachedAlias.get() != null) return;

    try {
      KeyStore ks = loadOrCreateKeyStore();
      String alias = readActiveAlias();

      if (alias == null) {
        if (!props.isCreateIfMissing()) throw new IllegalStateException("No active master alias");
        alias = createKeyAlias(ks, props.getAliasPrefix() + "1");
        persist(ks);
        writeActiveAlias(alias);
      }

      cachedAlias.set(alias);
      cachedKey.set(readKeyBytes(ks, alias));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to ensure active master key", e);
    }
  }

  @Override
  public byte[] getActiveMasterKey() {
    ensureActive();
    return cachedKey.get();
  }

  @Override
  public String getActiveMasterKeyVersion() {
    ensureActive();
    return cachedAlias.get();
  }

  @Override
  public byte[] getMasterKeyForVersion(String version) {
    try {
      KeyStore ks = loadOrCreateKeyStore();
      return readKeyBytes(ks, version);
    } catch (Exception e) {
      throw new IllegalStateException("Failed reading master key: " + version, e);
    }
  }

  @Override
  public synchronized String rotateMasterKey() {
    ensureActive();
    try {
      KeyStore ks = loadOrCreateKeyStore();
      String alias = props.getAliasPrefix() + nextVersion(ks);

      alias = createKeyAlias(ks, alias);
      persist(ks);
      writeActiveAlias(alias);

      cachedAlias.set(alias);
      cachedKey.set(readKeyBytes(ks, alias));
      return alias;
    } catch (Exception e) {
      throw new IllegalStateException("Failed rotating master key", e);
    }
  }

  // ---------- internal ----------

  private KeyStore loadOrCreateKeyStore() throws Exception {
    Path ksPath = Paths.get(req(props.getKeystorePath(), "keystorePath"));
    KeyStore ks = KeyStore.getInstance(props.getKeystoreType());
    char[] pwd = password();

    if (Files.exists(ksPath)) {
      try (InputStream in = Files.newInputStream(ksPath)) {
        ks.load(in, pwd);
      }
      return ks;
    }

    if (!props.isCreateIfMissing()) throw new IllegalStateException("Keystore missing: " + ksPath);
    Path parent = ksPath.getParent();
    if (parent != null) Files.createDirectories(parent);

    ks.load(null, pwd);
    return ks;
  }

  private void persist(KeyStore ks) throws Exception {
    Path ksPath = Paths.get(req(props.getKeystorePath(), "keystorePath"));
    try (OutputStream out =
        Files.newOutputStream(
            ksPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      ks.store(out, password());
    }
  }

  private String createKeyAlias(KeyStore ks, String alias) throws Exception {
    byte[] keyBytes = new byte[props.getMasterKeySizeBytes()];
    rng.nextBytes(keyBytes);
    SecretKey sk = new SecretKeySpec(keyBytes, "AES");

    KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(sk);
    KeyStore.ProtectionParameter prot = new KeyStore.PasswordProtection(password());
    ks.setEntry(alias, entry, prot);
    return alias;
  }

  private byte[] readKeyBytes(KeyStore ks, String alias) throws Exception {
    if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Blank alias");
    if (!ks.containsAlias(alias)) throw new IllegalStateException("Alias not found: " + alias);

    KeyStore.ProtectionParameter prot = new KeyStore.PasswordProtection(password());
    KeyStore.Entry entry = ks.getEntry(alias, prot);

    if (!(entry instanceof KeyStore.SecretKeyEntry skEntry)) {
      throw new IllegalStateException("Alias is not SecretKeyEntry: " + alias);
    }
    return skEntry.getSecretKey().getEncoded();
  }

  private int nextVersion(KeyStore ks) throws Exception {
    String prefix = props.getAliasPrefix();
    int max = 0;

    Enumeration<String> aliases = ks.aliases();
    while (aliases.hasMoreElements()) {
      String a = aliases.nextElement();
      if (!a.startsWith(prefix)) continue;

      String sfx = a.substring(prefix.length());
      try {
        max = Math.max(max, Integer.parseInt(sfx));
      } catch (NumberFormatException ignored) {
      }
    }
    return max + 1;
  }

  private String readActiveAlias() throws Exception {
    Path p = Paths.get(req(props.getActiveAliasFile(), "activeAliasFile"));
    if (!Files.exists(p)) return null;
    String s = Files.readString(p, StandardCharsets.UTF_8).trim();
    return s.isBlank() ? null : s;
  }

  private void writeActiveAlias(String alias) throws Exception {
    Path p = Paths.get(req(props.getActiveAliasFile(), "activeAliasFile"));
    Path parent = p.getParent();
    if (parent != null) Files.createDirectories(parent);

    Path tmp = Paths.get(p.toString() + ".tmp");
    Files.writeString(
        tmp,
        alias,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
    Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private char[] password() {
    if (props.getKeystorePassword() != null && !props.getKeystorePassword().isBlank()) {
      return props.getKeystorePassword().toCharArray();
    }
    String env = req(props.getKeystorePasswordEnv(), "keystorePasswordEnv");
    String v = System.getenv(env);
    if (v == null || v.isBlank()) throw new IllegalStateException("Missing env var: " + env);
    return v.toCharArray();
  }

  private static String req(String v, String name) {
    if (v == null || v.isBlank())
      throw new IllegalStateException("Missing master-key-store." + name);
    return v;
  }
}

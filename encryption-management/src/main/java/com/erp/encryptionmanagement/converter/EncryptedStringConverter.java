package com.erp.encryptionmanagement.converter;

import com.erp.encryptionmanagement.service.EnvelopeCryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JPA converter for String fields.
 *
 * <p>When no master key is configured (no MASTER_KEY_BASE64 and no keystore), values are stored as
 * plaintext rather than failing the whole save; {@link EnvelopeCryptoService#decrypt} already reads
 * plaintext rows back unchanged. Configure a master key to get encryption at rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private final EnvelopeCryptoService crypto;
  private final AtomicBoolean warnedNoKey = new AtomicBoolean();

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    try {
      return crypto.encrypt(attribute);
    } catch (IllegalStateException e) {
      if (warnedNoKey.compareAndSet(false, true)) {
        log.warn(
            "No master key available ({}); storing encrypted columns as plaintext. Set"
                + " MASTER_KEY_BASE64 or MASTER_KEYSTORE_PATH to enable encryption.",
            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
      }
      return attribute;
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return crypto.decrypt(dbData);
  }
}

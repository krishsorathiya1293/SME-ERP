package com.erp.encryptionmanagement.converter;

import com.erp.encryptionmanagement.service.EnvelopeCryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JPA converter for String fields. */
@Component
@RequiredArgsConstructor
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private final EnvelopeCryptoService crypto;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    return crypto.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return crypto.decrypt(dbData);
  }
}

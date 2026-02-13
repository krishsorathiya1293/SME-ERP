package com.erp.encryptionmanagement.attributeconverter;

import com.erp.encryptionmanagement.EncryptionServiceHolder;
import com.erp.encryptionmanagement.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptConverter implements AttributeConverter<String, String> {

  private EncryptionService service() {
    return EncryptionServiceHolder.get();
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return service().encryptIfNeeded(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return service().decryptIfNeeded(dbData);
  }
}

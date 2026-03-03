package com.erp.service;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractDocumentProvider<V extends Enum<V>> implements DocumentDataProvider {
  protected abstract Class<V> variantType();

  @Override
  public List<String> variants() {
    return Arrays.stream(variantType().getEnumConstants()).map(Enum::name).toList();
  }

  protected V toVariant(String variantString) {
    try {
      return Enum.valueOf(variantType(), variantString.toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(
          "Invalid variant '"
              + variantString
              + "' for form '"
              + formType()
              + "'. Expected one of: "
              + variants());
    }
  }
}

package com.erp.service;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractPdfDataProvider<V extends Enum<V>> implements PdfDataProvider {

  protected abstract Class<V> variantType();

  @Override
  public List<String> variants() {
    return Arrays.stream(variantType().getEnumConstants()).map(Enum::name).toList();
  }

  protected V toVariant(String variant) {
    try {
      return Enum.valueOf(variantType(), variant);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Unknown variant '" + variant + "' for " + formType() + ". Allowed: " + variants());
    }
  }
}

package com.erp.formsmanagement.domain.entity.invoice;

import lombok.Getter;

@Getter
public enum InvoiceType {
  EXPORT("EXPORT"),
  COMMERCIAL("COMMERCIAL"),
  PACKAGING_LIST("PACKAGING_LIST");

  private final String value;

  InvoiceType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

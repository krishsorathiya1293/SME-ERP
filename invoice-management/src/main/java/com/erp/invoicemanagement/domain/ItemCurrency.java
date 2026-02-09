package com.erp.invoicemanagement.domain;

import lombok.Getter;

@Getter
public enum ItemCurrency {
  USD("$"),
  EURO("€");

  private final String value;

  ItemCurrency(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

package com.erp.formsmanagement.domain.entity.master;

public enum StockStatus {
  IN_STOCK("IN_STOCK"),
  LOW_STOCK("LOW_STOCK");

  private final String value;

  StockStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

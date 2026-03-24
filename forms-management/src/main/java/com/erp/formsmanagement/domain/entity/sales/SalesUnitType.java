package com.erp.formsmanagement.domain.entity.sales;

import lombok.Getter;

@Getter
public enum SalesUnitType {
  KG("KG"),
  PC("PC");

  private final String value;

  SalesUnitType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

package com.erp.formsmanagement.domain.entity.purchase;

import lombok.Getter;

@Getter
public enum PurchaseUnitType {
  KG("KG"),
  PC("PC");

  private final String value;

  PurchaseUnitType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

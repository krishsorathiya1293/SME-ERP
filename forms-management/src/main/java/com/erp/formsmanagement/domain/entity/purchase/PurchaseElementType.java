package com.erp.formsmanagement.domain.entity.purchase;

import lombok.Getter;

@Getter
public enum PurchaseElementType {
  WOODEN_PETI("Wooden Peti"),
  PETI("Peti"),
  BAG("Bag"),
  HEAVY_PETI("Heavy Peti");

  private final String value;

  PurchaseElementType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

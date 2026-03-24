package com.erp.formsmanagement.domain.entity.sales;

import lombok.Getter;

@Getter
public enum SalesElementType {
  WOODEN_PETI("Wooden Peti"),
  PETI("Peti"),
  BAG("Bag"),
  HEAVY_PETI("Heavy Peti");

  private final String value;

  SalesElementType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

package com.erp.formsmanagement.domain.entity.order;

import lombok.Getter;

@Getter
public enum ElementType {
  PETI("Peti"),
  DRUM("Drum");

  private final String value;

  ElementType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

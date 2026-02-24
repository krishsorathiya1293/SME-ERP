package com.erp.formsmanagement.domain.entity.master;

import lombok.Getter;

@Getter
public enum PartyType {
  CUSTOMER("CUSTOMER"),
  VENDOR("VENDOR"),
  BOTH("BOTH");

  private final String value;

  PartyType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

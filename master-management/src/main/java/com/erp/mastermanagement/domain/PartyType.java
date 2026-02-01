package com.erp.mastermanagement.domain;

import lombok.Getter;

@Getter
public enum PartyType {
  CUSTOMER("CUSTOMER"),
  VENDOR("VENDOR");

  private final String value;

  PartyType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

package com.erp.formsmanagement.domain.entity.order;

import lombok.Getter;

@Getter
public enum JobWorkStatus {
  COMPLETE("Complete"),
  PENDING("Pending"),
  REJECT("Reject");

  private final String value;

  JobWorkStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

package com.erp.formsmanagement.domain.entity.order;

import lombok.Getter;

@Getter
public enum JobPlatingType {
  JOB_WORK("Job Work"),
  IN_HOUSE("In House");

  private final String value;

  JobPlatingType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

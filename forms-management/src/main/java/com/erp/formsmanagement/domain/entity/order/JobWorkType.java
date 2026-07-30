package com.erp.formsmanagement.domain.entity.order;

import lombok.Getter;

@Getter
public enum JobWorkType {
  INHOUSE("In House"),
  OUTSIDE("Outside"),
  JOB_WORK("Job Work"),
  MANUAL("Manual");

  private final String value;

  JobWorkType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

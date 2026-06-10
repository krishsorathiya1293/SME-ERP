package com.erp.formsmanagement.clientportal.domain.entity;

/** Status lifecycle for a client order request submitted from the Client Portal "Shop". */
public enum ClientOrderRequestStatus {
  PENDING_APPROVAL,
  APPROVED,
  IN_PROGRESS,
  COMPLETED,
  REJECTED
}

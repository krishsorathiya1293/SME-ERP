package com.erp.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constant {
  public static final String ENTITY_NOT_FOUND = "record not found with id: %d";
  public static final String INVOICE_FILENAME_FORMAT = "%s (%s) %s";
}

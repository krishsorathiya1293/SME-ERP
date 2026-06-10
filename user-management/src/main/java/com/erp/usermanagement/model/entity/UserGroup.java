package com.erp.usermanagement.model.entity;

import lombok.Getter;

/**
 * Enum representing user groups in the system. Each user belongs to one user group which determines
 * their access level.
 */
@Getter
public enum UserGroup {
  ADMIN("Admin"),
  CLIENT("Client");

  private final String value;

  UserGroup(String value) {
    this.value = value;
  }

  /**
   * Convert string value to UserGroup enum.
   *
   * @param value The string value to convert
   * @return The matching UserGroup
   * @throws IllegalArgumentException if value doesn't match any UserGroup
   */
  public static UserGroup fromValue(String value) {
    for (UserGroup group : UserGroup.values()) {
      if (group.value.equals(value)) {
        return group;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

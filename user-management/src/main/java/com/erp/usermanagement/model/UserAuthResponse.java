package com.erp.usermanagement.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAuthResponse {
  private String accessToken;

  private String refreshToken;
}

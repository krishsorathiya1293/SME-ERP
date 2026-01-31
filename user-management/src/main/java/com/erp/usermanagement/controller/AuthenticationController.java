package com.erp.usermanagement.controller;

import com.erp.api.usermanagement.AuthenticationUserManagementApi;
import com.erp.api.usermanagement.model.AuthResponse;
import com.erp.api.usermanagement.model.RefreshTokenRequest;
import com.erp.api.usermanagement.model.SignInRequest;
import com.erp.usermanagement.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Controller for authentication endpoints (signin and refresh token). */
@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationUserManagementApi {

  private final AuthenticationService authenticationService;

  @Override
  public ResponseEntity<AuthResponse> signIn(SignInRequest signInRequest) {
    return ResponseEntity.ok(
        authenticationService.authenticate(
            signInRequest.getUsername(), signInRequest.getPassword()));
  }

  @Override
  public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    return ResponseEntity.ok(
        authenticationService.refreshToken(refreshTokenRequest.getRefreshToken()));
  }
}

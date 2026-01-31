package com.erp.usermanagement.service;

import com.erp.api.usermanagement.model.AuthResponse;
import com.erp.usermanagement.mapper.UserMapper;
import com.erp.usermanagement.model.UserAuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/** Service for handling user authentication and token refresh operations. */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenService jwtTokenService;
  private final UserMapper userMapper;
  private final UserDetailsService userDetailsService;

  public AuthResponse authenticate(String username, String password) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    return userMapper.toAuthResponse(
        UserAuthResponse.builder()
            .accessToken(jwtTokenService.generateAccessToken(userDetails))
            .refreshToken(jwtTokenService.generateRefreshToken(userDetails))
            .build());
  }

  /**
   * Refresh access token using a valid refresh token. Returns new access token and the same refresh
   * token.
   */
  public AuthResponse refreshToken(String refreshToken) {
    // Extract username from refresh token
    String username = jwtTokenService.extractUsername(refreshToken);

    // Load user details
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // Validate refresh token
    if (!jwtTokenService.isRefreshTokenValid(refreshToken, userDetails)) {
      throw new IllegalArgumentException("Invalid refresh token");
    }

    return userMapper.toAuthResponse(
        UserAuthResponse.builder()
            .accessToken(jwtTokenService.generateAccessToken(userDetails))
            .refreshToken(refreshToken) // Return same refresh token
            .build());
  }
}

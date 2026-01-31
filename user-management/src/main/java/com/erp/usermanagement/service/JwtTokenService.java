package com.erp.usermanagement.service;

import com.erp.usermanagement.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation, validation, and claim extraction. Supports both access and
 * refresh tokens with role-based claims.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {
  private final JwtProperties jwtProperties;
  private SecretKey signingKey;

  @PostConstruct
  private void initKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
    signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /** Extract username from token */
  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  /** Extract roles from token */
  public List<String> extractRoles(String token) {
    Claims claims = extractAllClaims(token);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> roles = claims.get("roles", List.class);
    if (roles != null) {
      List<String> list = new ArrayList<>();
      for (Map<String, String> role : roles) {
        String authority = role.get("authority");
        list.add(authority);
      }
      return list;
    } else {
      return List.of();
    }
  }

  /** Check if token is a refresh token */
  public boolean isRefreshToken(String token) {
    Claims claims = extractAllClaims(token);
    return "refresh".equals(claims.get("type"));
  }

  /** Generate access token */
  public String generateAccessToken(UserDetails userDetails) {
    return generateToken(
        Map.of("type", "access"), userDetails, jwtProperties.getAccessTokenValidity());
  }

  /** Generate refresh token */
  public String generateRefreshToken(UserDetails userDetails) {
    return generateToken(
        Map.of("type", "refresh"), userDetails, jwtProperties.getRefreshTokenValidity());
  }

  /** Validate token and match username */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /** Validate refresh token specifically */
  public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
    return isRefreshToken(token) && isTokenValid(token, userDetails);
  }

  private String generateToken(
      Map<String, Object> extraClaims, UserDetails userDetails, long validity) {
    long now = System.currentTimeMillis();

    // Extract roles as simple strings
    List<String> roles = new ArrayList<>();
    for (GrantedAuthority grantedAuthority : userDetails.getAuthorities()) {
      String authority = grantedAuthority.getAuthority();
      roles.add(authority);
    }

    return Jwts.builder()
        .claims(extraClaims)
        .subject(userDetails.getUsername())
        .claim("roles", roles)
        .issuedAt(new Date(now))
        .expiration(new Date(now + validity))
        .signWith(signingKey)
        .compact();
  }

  private boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}

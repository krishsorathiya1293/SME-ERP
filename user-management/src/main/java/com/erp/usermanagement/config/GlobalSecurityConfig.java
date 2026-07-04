package com.erp.usermanagement.config;

import com.erp.usermanagement.security.JwtAuthenticationFilter;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Main security configuration for the application. Configures JWT-based authentication, role-based
 * authorization, and security filter chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
public class GlobalSecurityConfig {

  private final SecurityProperties securityProperties;
  private final CorsProperties corsProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(securityProperties.getPublicAccess().toArray(String[]::new))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    for (CorsProperties.CorsMapping mapping : corsProperties.getMappings()) {
      CorsConfiguration config = new CorsConfiguration();

      if (Objects.nonNull(mapping.getAllowedOrigins())) {
        // FIX: Flatten and split the origins to handle comma-separated strings from .env
        java.util.List<String> flatOrigins =
            mapping.getAllowedOrigins().stream()
                .flatMap(origin -> java.util.Arrays.stream(origin.split(",")))
                .map(String::trim)
                .toList();
        config.setAllowedOrigins(flatOrigins);
      }

      config.setAllowedMethods(mapping.getAllowedMethods());
      config.setAllowCredentials(mapping.getAllowCredentials());

      // Ensure headers are allowed for JWT, plus X-Party-Id used by group logins to select the
      // company/party they are currently acting as.
      config.setAllowedHeaders(
          java.util.List.of("Authorization", "Content-Type", "Cache-Control", "X-Party-Id"));
      config.setExposedHeaders(java.util.List.of("Authorization")); // Important for some frontends

      source.registerCorsConfiguration(mapping.getPath(), config);
    }
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

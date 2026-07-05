package com.erp.usermanagement.service;

import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of UserDetailsService that loads users from the database. Converts UserEntity to
 * Spring Security UserDetails for authentication.
 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserEntity user =
        userRepository
            .findByUsernameOrUserEmail(username, username)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with username or email: " + username));

    // Note: we intentionally do NOT throw here for a disabled account. The `.disabled(...)` flag
    // below makes Spring raise a DisabledException, which the exception handler turns into a clear
    // "login is disabled" message instead of the generic "invalid username or password".
    String authority = "ROLE_" + user.getUserGroup().name();

    return User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(Collections.singletonList(new SimpleGrantedAuthority(authority)))
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .disabled(!user.getEnabled())
        .build();
  }
}

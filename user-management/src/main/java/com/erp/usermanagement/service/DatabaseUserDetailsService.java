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

    if (Boolean.FALSE.equals(user.getEnabled())) {
      throw new UsernameNotFoundException("User account is disabled");
    }
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

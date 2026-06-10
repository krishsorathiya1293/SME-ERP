package com.erp.formsmanagement.clientportal.service;

import com.erp.exception.EntityNotFoundException;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the {@link UserEntity} for the currently authenticated client user. */
@Component
@RequiredArgsConstructor
public class CurrentClientProvider {

  private final UserRepository userRepository;

  public UserEntity getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
  }
}

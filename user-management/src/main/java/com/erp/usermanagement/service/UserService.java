package com.erp.usermanagement.service;

import com.erp.api.usermanagement.model.PaginatedResultUser;
import com.erp.api.usermanagement.model.User;
import com.erp.usermanagement.mapper.UserMapper;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.model.entity.UserGroup;
import com.erp.usermanagement.repository.UserRepository;
import com.erp.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations. Handles user CRUD operations, registration, and user
 * group management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  /** Register a new user with the given email, password, and user group. */
  @Transactional
  public UserEntity registerUser(String email, String password, String userGroup) {
    if (userRepository.existsByUserEmail(email)) {
      throw new IllegalArgumentException("User with email " + email + " already exists");
    }

    UserEntity user =
        UserEntity.builder()
            .userEmail(email)
            .password(passwordEncoder.encode(password))
            .userGroup(UserGroup.fromValue(userGroup))
            .enabled(true)
            .build();

    return userRepository.save(user);
  }

  /** Get user by ID. */
  public UserEntity getUserById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
  }

  public PaginatedResultUser getPeginatedUsers(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<String> sortDirection,
      Optional<String> sortBy) {
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<UserEntity> userEntities =
        search
            .map(
                query -> userRepository.findByUserEmailContainingIgnoreCase(query.trim(), pageable))
            .orElseGet(() -> userRepository.findAll(pageable));
    List<User> userList = userMapper.toUserResponseList(userEntities.getContent());
    return new PaginatedResultUser()
        .data(userList)
        .empty(userList.isEmpty())
        .last(userEntities.isLast())
        .first(userEntities.isFirst())
        .totalPages(userEntities.getTotalPages())
        .size(userList.size())
        .totalElements((int) userEntities.getTotalElements());
  }

  /** Update user's group. */
  @Transactional
  public UserEntity updateUserGroup(Long userId, UserGroup newGroup) {
    UserEntity user = getUserById(userId);
    user.setUserGroup(newGroup);
    return userRepository.save(user);
  }

  /** Delete user by ID. */
  @Transactional
  public void deleteUser(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new IllegalArgumentException("User not found with id: " + userId);
    }
    userRepository.deleteById(userId);
  }
}

package com.erp.usermanagement.service;

import com.erp.api.usermanagement.model.PaginatedResultUser;
import com.erp.api.usermanagement.model.User;
import com.erp.exception.EntityNotFoundException;
import com.erp.usermanagement.mapper.UserMapper;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.model.entity.UserGroup;
import com.erp.usermanagement.repository.UserRepository;
import com.erp.util.PaginationUtils;
import java.security.SecureRandom;
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

  private static final String DEFAULT_CLIENT_USERNAME = "client";
  private static final int MAX_USERNAME_BASE_LENGTH = 20;
  private static final String PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  private static final int GENERATED_PASSWORD_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

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
            .username(email)
            .userEmail(email)
            .password(passwordEncoder.encode(password))
            .userGroup(UserGroup.fromValue(userGroup))
            .enabled(true)
            .build();

    return userRepository.save(user);
  }

  /**
   * Auto-create a CLIENT user account for a newly created party. Generates a unique login
   * username derived from the party's name and a random password; the email and other profile
   * fields remain empty until the client fills them in. The plaintext password is retained
   * (encrypted at rest) so the admin can download it for the party until the client changes it.
   */
  @Transactional
  public UserEntity registerClientUser(Long partyId, String partyName) {
    String username = generateClientUsername(partyName);
    String rawPassword = generateRandomPassword();

    UserEntity user =
        UserEntity.builder()
            .username(username)
            .partyId(partyId)
            .password(passwordEncoder.encode(rawPassword))
            .initialPassword(rawPassword)
            .userGroup(UserGroup.CLIENT)
            .enabled(true)
            .build();

    return userRepository.save(user);
  }

  /**
   * Change a user's password after verifying their current password. Clears the stored initial
   * password so the account no longer shows up as "pending" in the admin credentials export.
   */
  @Transactional
  public void changePassword(Long userId, String currentPassword, String newPassword) {
    UserEntity user = getUserById(userId);
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    user.setInitialPassword(null);
    userRepository.save(user);
  }

  /**
   * Regenerate the login credentials for the CLIENT user linked to the given party. Returns the
   * updated user entity with the new plaintext password set as the initial password.
   */
  @Transactional
  public UserEntity resetClientCredentials(Long partyId) {
    UserEntity user =
        userRepository
            .findByPartyId(partyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No client account found for party id: " + partyId));

    String rawPassword = generateRandomPassword();
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setInitialPassword(rawPassword);
    return userRepository.save(user);
  }

  /**
   * Auto-create the single group-level CLIENT login for a newly created party group. The credential
   * is shared across all the group's member companies; the client picks which company to act as
   * after logging in. Mirrors {@link #registerClientUser} but links to a group instead of a party.
   */
  @Transactional
  public UserEntity registerGroupUser(Long groupId, String groupName) {
    String username = generateClientUsername(groupName);
    String rawPassword = generateRandomPassword();

    UserEntity user =
        UserEntity.builder()
            .username(username)
            .groupId(groupId)
            .password(passwordEncoder.encode(rawPassword))
            .initialPassword(rawPassword)
            .userGroup(UserGroup.CLIENT)
            .enabled(true)
            .build();

    return userRepository.save(user);
  }

  /**
   * Regenerate the login credentials for the group-level CLIENT login linked to the given party
   * group. Returns the updated user entity with the new plaintext password set as the initial
   * password.
   */
  @Transactional
  public UserEntity resetGroupCredentials(Long groupId) {
    UserEntity user =
        userRepository
            .findByGroupId(groupId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No group account found for group id: " + groupId));

    String rawPassword = generateRandomPassword();
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setInitialPassword(rawPassword);
    return userRepository.save(user);
  }

  /**
   * Build a unique username derived from the party's name (e.g. "Acme Trading Co." -&gt;
   * "acmetradingco"). If the slugified name is blank, falls back to "client". If the resulting
   * username is already taken, a numeric suffix is appended until it is unique.
   */
  private String generateClientUsername(String partyName) {
    String base = slugify(partyName);
    if (base.isBlank()) {
      base = DEFAULT_CLIENT_USERNAME;
    }

    String username = base;
    int suffix = 1;
    while (userRepository.existsByUsername(username)) {
      suffix++;
      username = base + suffix;
    }
    return username;
  }

  /** Lowercase, strip everything but letters/digits, and cap the length. */
  private String slugify(String value) {
    if (value == null) {
      return "";
    }
    String slug = value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
    if (slug.length() > MAX_USERNAME_BASE_LENGTH) {
      slug = slug.substring(0, MAX_USERNAME_BASE_LENGTH);
    }
    return slug;
  }

  private String generateRandomPassword() {
    StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
    for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
      sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
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

  /**
   * Remove the auto-created CLIENT login linked to a party, if one exists. Called when a party is
   * deleted: every party gets a client login on creation ({@link #registerClientUser}) and its
   * {@code users.party_id} foreign key has no cascade, so the login must be cleared first or the
   * party delete fails even when the party has no orders. No-op when the party has no login.
   */
  @Transactional
  public void deleteClientUserByPartyId(Long partyId) {
    userRepository.findByPartyId(partyId).ifPresent(userRepository::delete);
  }
}

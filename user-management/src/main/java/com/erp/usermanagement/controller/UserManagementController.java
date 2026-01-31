package com.erp.usermanagement.controller;

import com.erp.api.usermanagement.UserManagementUserManagementApi;
import com.erp.api.usermanagement.model.PaginatedResultUser;
import com.erp.api.usermanagement.model.UpdateUserGroupRequest;
import com.erp.api.usermanagement.model.User;
import com.erp.api.usermanagement.model.UserRegistrationRequest;
import com.erp.usermanagement.mapper.UserMapper;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.model.entity.UserGroup;
import com.erp.usermanagement.security.roles.Admin;
import com.erp.usermanagement.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Controller for user management operations. */
@RestController
@RequiredArgsConstructor
public class UserManagementController implements UserManagementUserManagementApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<User> registerUser(UserRegistrationRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            userMapper.toUserResponse(
                userService.registerUser(
                    request.getEmail(), request.getPassword(), request.getUserGroup().getValue())));
  }

  @Override
  @Admin
  public ResponseEntity<PaginatedResultUser> getAllUsers(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<String> sortDirection,
      Optional<String> sortBy) {
    return ResponseEntity.ok(
        userService.getPeginatedUsers(page, size, search, sortDirection, sortBy));
  }

  @Override
  @Admin
  public ResponseEntity<User> getUserById(Long id) {
    return ResponseEntity.ok(userMapper.toUserResponse(userService.getUserById(id)));
  }

  @Override
  @Admin
  public ResponseEntity<Void> deleteUser(Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Admin
  public ResponseEntity<User> updateUserGroup(Long id, UpdateUserGroupRequest request) {
    UserEntity user =
        userService.updateUserGroup(id, UserGroup.valueOf(request.getUserGroup().getValue()));
    return ResponseEntity.ok(userMapper.toUserResponse(user));
  }
}

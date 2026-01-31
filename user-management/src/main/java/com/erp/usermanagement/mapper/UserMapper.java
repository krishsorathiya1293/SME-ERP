package com.erp.usermanagement.mapper;

import com.erp.api.usermanagement.model.AuthResponse;
import com.erp.api.usermanagement.model.User;
import com.erp.usermanagement.model.UserAuthResponse;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.model.entity.UserGroup;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {
  AuthResponse toAuthResponse(UserAuthResponse userAuthResponse);

  @Mapping(source = "userEmail", target = "email")
  @Mapping(source = "userGroup", target = "userGroup", qualifiedByName = "mapUserGroup")
  User toUserResponse(UserEntity user);

  List<User> toUserResponseList(List<UserEntity> users);

  @Named("mapUserGroup")
  default com.erp.api.usermanagement.model.UserGroup mapUserGroup(UserGroup group) {
    if (group == null) return null;
    return com.erp.api.usermanagement.model.UserGroup.valueOf(group.name());
  }
}

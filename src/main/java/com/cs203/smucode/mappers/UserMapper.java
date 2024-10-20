package com.cs203.smucode.mappers;

import com.cs203.smucode.models.User;
import com.cs203.smucode.dto.UserDTO;
import com.cs203.smucode.models.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(source = "userRole", target = "role", qualifiedByName = "userRoleToRole")
    @Mapping(source = "password", target = "password", ignore = true)
    UserDTO userToUserDTO(User user);

    @Mapping(source = "role", target = "userRole", qualifiedByName = "roleToUserRole")
    User userDTOtoUser(UserDTO userDTO);

    @Named("roleToUserRole")
    default UserRole stringToUserType(String role) {
        return UserRole.valueOf(role.toUpperCase()); // Matches case-insensitively
    }

    @Named("userRoleToRole")
    default String userTypeToString(UserRole role) {
        return role.toString();
    }
}

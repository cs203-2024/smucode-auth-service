package com.cs203.smucode.services;

import com.cs203.smucode.models.User;

import java.util.UUID;

public interface IUserService {
    User getUserByUsername(String username);
    User createUser(User user);
    void updatePassword(String username, String oldPassword, String newPassword);
    void deleteUser(UUID id);
    void updateEmail(UUID id, String email);
}
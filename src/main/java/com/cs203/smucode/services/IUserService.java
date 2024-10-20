package com.cs203.smucode.services;

import com.cs203.smucode.models.User;

import java.util.UUID;
public interface IUserService {
    User getUserByUsername(String username);
    void createUser(User user);
    void updatePassword(UUID id, String oldPassword, String newPassword);
    void deleteUser(UUID id, String password);
}
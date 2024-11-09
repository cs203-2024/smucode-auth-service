package com.cs203.smucode.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequestDTO (
    @NotNull(message = "Username cannot be empty/null")
    String username,

    @NotNull(message = "Old Password cannot be empty/null")
    String oldPassword,

    @NotNull(message = "Password cannot be empty/null")
    @Size(min = 8, message = "Password must be more than 8 characters")
    @Pattern(message = "New Password must contain 1 symbol, 1 uppercase, 1 lowercase and 1 digit",
            regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).*$")
    String newPassword
) {}
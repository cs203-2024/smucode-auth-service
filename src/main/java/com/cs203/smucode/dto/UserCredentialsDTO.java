package com.cs203.smucode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserCredentialsDTO (
    @NotNull(message = "Username cannot be empty/null")
    UUID id,

    @NotNull(message = "Password cannot be empty/null")
    @Size(min = 8, message = "Password must be more than 8 characters")
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]*$")
    String oldPassword,

    @NotNull(message = "Password cannot be empty/null")
    @Size(min = 8, message = "Password must be more than 8 characters")
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]*$")
    String newPassword,

    @NotNull(message = "Email cannot be empty/null")
    @Email(message = "Invalid email format")
    String email
) {}
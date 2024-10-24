package com.cs203.smucode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserIdentificationDTO(
        @NotNull(message = "UUID cannot be null")
        UUID id,

        @NotNull(message = "Username cannot be empty/null")
        String username,

        @NotNull(message = "Email cannot be empty/null")
        @Email(message = "Invalid email format")
        String email
        ) {
}

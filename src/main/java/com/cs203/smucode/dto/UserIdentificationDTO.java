package com.cs203.smucode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserIdentificationDTO(
        @NotNull(message = "Username cannot be empty/null")
        String username,

        @NotNull(message = "Email cannot be empty/null")
        @Email(message = "Invalid email format")
        String email
        ) {
}

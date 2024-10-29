package com.cs203.smucode.dto;

import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "Username cannot be empty/null")
        String username,

        @NotNull(message = "Password cannot be empty/null")
        String password
) {
}

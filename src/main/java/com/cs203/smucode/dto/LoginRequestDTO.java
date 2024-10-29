package com.cs203.smucode.dto;

import jakarta.validation.constraints.NotNull;
public record LoginRequestDTO(
        @NotNull(message = "Invalid login credentials")
        String username,

        @NotNull(message = "Invalid login credentials")
        String password
) {
}

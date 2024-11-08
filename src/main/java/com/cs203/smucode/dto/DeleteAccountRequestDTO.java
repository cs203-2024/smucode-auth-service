package com.cs203.smucode.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DeleteAccountRequestDTO(
        @NotNull(message = "Password cannot be null")
        @NotEmpty(message = "Password cannot be empty")
        String password
) {
}

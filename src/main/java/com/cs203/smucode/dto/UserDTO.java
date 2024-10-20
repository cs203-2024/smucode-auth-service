package com.cs203.smucode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UserDTO (

        @NotNull(message = "Username cannot be empty/null")
        String username,

        @NotNull(message = "Password cannot be empty/null")
        @Size(min = 8, message = "Password must be more than 8 characters")
        @Pattern(message = "Password must contain 1 symbol, 1 uppercase, 1 lowercase and 1 digit",
                regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).*$")
        String password,

        @NotNull(message = "Email cannot be empty/null")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role cannot be empty/null")
        String role

) {}

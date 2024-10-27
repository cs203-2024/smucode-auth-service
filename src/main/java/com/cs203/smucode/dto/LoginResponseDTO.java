package com.cs203.smucode.dto;

public record LoginResponseDTO(
        String message,
        UserDTO userDTO
) {}

package com.cs203.smucode.dto;

public record JWTResponseDTO(
        String message,
        UserDTO userDTO,
        String token
) {}

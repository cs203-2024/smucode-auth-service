package com.cs203.smucode.services;

import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.utils.JWTUtil;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface ITokenService {
    String createRefreshToken(String username);
    RefreshToken validateRefreshToken(UUID refreshToken);
    void blacklistRefreshToken(UUID refreshToken);
    String createAccessToken(UserDetails userDetails);
    JWTUtil getJWTUtil();
}
package com.cs203.smucode.services.impl;

import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.models.User;
import com.cs203.smucode.repositories.RefreshTokenRepository;
import com.cs203.smucode.repositories.UserRepository;
import com.cs203.smucode.services.ITokenService;
import com.cs203.smucode.utils.JWTUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TokenServiceImpl implements ITokenService {

    @Value("${jwt.refresh.duration}")
    private long REFRESH_TOKEN_VALIDITY;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    @Autowired
    public TokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository, JWTUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    // username as if we redirect request, uuid may not be in every request?
    public String createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        final UUID tokenId = UUID.randomUUID();
        final RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(tokenId);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(REFRESH_TOKEN_VALIDITY)));

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken().toString();
    }

    @Override
    @Transactional
    public RefreshToken validateRefreshToken(UUID refreshToken) {
        return refreshTokenRepository
                .findByTokenAndExpiresAtAfter(refreshToken, OffsetDateTime.now())
                .orElse(null);
    }

    @Override
    @Transactional
    public void blacklistRefreshToken(UUID refreshToken) {
        refreshTokenRepository.findByTokenAndExpiresAtAfter(refreshToken, OffsetDateTime.now())
                .ifPresent( invalidToken -> {
                    // Set expiration to now to invalidate the token
                    invalidToken.setExpiresAt(OffsetDateTime.now());
                    refreshTokenRepository.save(invalidToken);
                });
    }

    @Override
    @Transactional
    public String createAccessToken(UserDetails userDetails) {
        return jwtUtil.generateToken(userDetails);
    }


    @Override
    public JWTUtil getJWTUtil() {
        return this.jwtUtil;
    }

}

package com.cs203.smucode.services.impl;

import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.models.User;
import com.cs203.smucode.repositories.RefreshTokenRepository;
import com.cs203.smucode.repositories.UserRepository;
import com.cs203.smucode.services.IRefreshTokenService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    @Value("${jwt.refresh.duration}")
    private Duration REFRESH_TOKEN_VALIDITY = Duration.ofMinutes(120);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    // username as if we redirect request, uuid may not be in every request?
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        final UUID tokenId = UUID.randomUUID();
        final RefreshToken refreshToken = new RefreshToken();

        refreshToken.setId(tokenId);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(OffsetDateTime.now().plus(REFRESH_TOKEN_VALIDITY));

        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    @Override
    @Transactional
    public boolean validateRefreshToken(UUID refreshToken) {
        return refreshTokenRepository
                .findByTokenAndExpiresAtAfter(refreshToken, OffsetDateTime.now())
                .isPresent();
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
}

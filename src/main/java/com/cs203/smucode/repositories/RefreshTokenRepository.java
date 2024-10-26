package com.cs203.smucode.repositories;

import com.cs203.smucode.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenAndExpiresAtAfter(UUID token, OffsetDateTime expiresAt);
}

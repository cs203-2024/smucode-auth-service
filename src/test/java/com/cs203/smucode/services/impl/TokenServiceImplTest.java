package com.cs203.smucode.services.impl;

import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.models.User;
import com.cs203.smucode.repositories.RefreshTokenRepository;
import com.cs203.smucode.repositories.UserRepository;
import com.cs203.smucode.utils.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTUtil jwtUtil;

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Test
    void createRefreshToken_ValidUsername_Success() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        ReflectionTestUtils.setField(tokenService, "refreshTokenValidity", 60L);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        String result = tokenService.createRefreshToken(username);

        // Assert
        assertNotNull(result);
        assertTrue(UUID.fromString(result) instanceof UUID);

        // Verify
        verify(userRepository).findByUsername(username);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_InvalidUsername_ThrowsException() {
        // Arrange
        String username = "nonexistent";

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> tokenService.createRefreshToken(username));

        // Verify
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void validateRefreshToken_ValidToken_ReturnsRefreshToken() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenId);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));

        // Stub
        when(refreshTokenRepository.findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(refreshToken));

        // Act
        RefreshToken result = tokenService.validateRefreshToken(tokenId);

        // Assert
        assertNotNull(result);
        assertEquals(tokenId, result.getToken());

        // Verify
        verify(refreshTokenRepository).findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class));
    }

    @Test
    void validateRefreshToken_ExpiredToken_ReturnsNull() {
        // Arrange
        UUID tokenId = UUID.randomUUID();

        // Stub
        when(refreshTokenRepository.findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        RefreshToken result = tokenService.validateRefreshToken(tokenId);

        // Assert
        assertNull(result);

        // Verify
        verify(refreshTokenRepository).findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class));
    }

    @Test
    void blacklistRefreshToken_ValidToken_TokenInvalidated() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenId);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));

        // Stub
        when(refreshTokenRepository.findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        tokenService.blacklistRefreshToken(tokenId);

        // Assert & Verify
        verify(refreshTokenRepository).findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertTrue(refreshToken.getExpiresAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
    }

    @Test
    void blacklistRefreshToken_ExpiredToken_NoAction() {
        // Arrange
        UUID tokenId = UUID.randomUUID();

        // Stub
        when(refreshTokenRepository.findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        tokenService.blacklistRefreshToken(tokenId);

        // Verify
        verify(refreshTokenRepository).findByTokenAndExpiresAtAfter(eq(tokenId), any(OffsetDateTime.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void createAccessToken_Success() {
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        String expectedToken = "jwt.token.here";

        // Stub
        when(jwtUtil.generateToken(userDetails)).thenReturn(expectedToken);

        // Act
        String result = tokenService.createAccessToken(userDetails);

        // Assert
        assertEquals(expectedToken, result);

        // Verify
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    void getJwtUtil_ReturnsJwtUtil() {
        // Act
        JWTUtil result = tokenService.getJWTUtil();

        // Assert
        assertEquals(jwtUtil, result);
    }
}
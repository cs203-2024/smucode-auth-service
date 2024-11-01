package com.cs203.smucode.controllers;

import com.cs203.smucode.constants.TimeConstants;
import com.cs203.smucode.dto.*;
import com.cs203.smucode.exception.ApiRequestException;
import com.cs203.smucode.exception.InvalidTokenException;
import com.cs203.smucode.mappers.UserMapper;
import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.models.User;
import com.cs203.smucode.services.ITokenService;
import com.cs203.smucode.services.IUserService;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author: gav
 * @version: 1.0
 * @since: 2024-10-17
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${https.enabled}")
    private boolean httpsEnabled;

    @Value("${jwt.refresh.duration}")
    private long refreshDurationInMinutes;

    @Value("${jwt.access.duration}")
    private long accessDurationInSeconds;

    private final IUserService userService;
    private final ITokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AuthController(IUserService userService,
                          AuthenticationManager authenticationManager,
                          ITokenService tokenService,
                          UserDetailsService userDetailsService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        if (dto.username() == null || dto.username().isEmpty()) {
            throw new ApiRequestException("Username cannot be null or empty");
        }

        if (dto.password() == null || dto.password().isEmpty()) {
            throw new ApiRequestException("Password is required");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
            );

            if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
                throw new IllegalArgumentException("Expected userDetails, got" + authentication.getPrincipal().getClass().getName());
            }

            String accessToken = tokenService.createAccessToken(userDetails);
            String refreshToken = tokenService.createRefreshToken(dto.username());

            List<ResponseCookie> refreshTokenCookies = this.buildRefreshTokenForCookie(
                    refreshToken,refreshDurationInMinutes * TimeConstants.SECONDS
            );

            ResponseCookie accessTokenCookie = this.buildAccessTokenForCookie(
                    accessToken, accessDurationInSeconds
            );

            UserDTO userDTO = UserMapper.INSTANCE.userToUserDTO(userService.getUserByUsername(dto.username()));

            return ResponseEntity.ok()
                    .headers(headers -> {
                        for (ResponseCookie cookie : refreshTokenCookies) {
                            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
                        }
                        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                    })
                    .body(new LoginResponseDTO("success", userDTO));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO("Invalid username or password", null));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO("Ensure that you have typed the username and password correctly", null));
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during login");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody @Valid UserDTO dto) {
        // Validate request
        if (dto.username() == null || dto.username().isEmpty()) {
            throw new ApiRequestException("Username cannot be null or empty");
        }

        if (dto.password() == null || dto.password().isEmpty()) {
            throw new ApiRequestException("Password is required");
        }

        try {
            // Save the user with their authorities
            User createdUser = UserMapper.INSTANCE.userDTOtoUser(dto);
            userService.createUser(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserMapper.INSTANCE.userToUserDTO(createdUser));
        } catch (DataIntegrityViolationException e) {
            throw new ApiRequestException("That username/email already exists, please try another");
        } catch (ValidationException e) {
            throw new ApiRequestException(e.getMessage());
        } catch (ResourceAccessException e) {
            throw new ApiRequestException("Something went wrong on our end, please try again");
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during signup");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(value="refreshToken", required = false) String refreshTokenId) {
        try {
            tokenService.blacklistRefreshToken(
                    UUID.fromString(refreshTokenId)
            );

            String refreshToken = "destroyedRefresh";
            List<ResponseCookie> refreshTokenCookies = this.buildRefreshTokenForCookie(
                    refreshToken, TimeConstants.NOW
            );

            String accessToken = "destroyedAccess";
            ResponseCookie accessTokenCookie = this.buildAccessTokenForCookie(
                    accessToken, TimeConstants.NOW
            );

            return ResponseEntity.ok()
                    .headers(headers -> {
                        for (ResponseCookie cookie : refreshTokenCookies) {
                            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
                        }
                        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                    })
                    .body("User logged out successfully");
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during logout");
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(@RequestBody @Valid LoginRequestDTO dto) {
        if (dto.username() == null) {
            throw new ApiRequestException("Username cannot be null or empty");
        }

        try {
            userService.deleteUser(dto.username(), dto.password());
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during delete account");
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid UserCredentialsDTO dto) {

        if (dto.username() == null) {
            throw new ApiRequestException("Username cannot be null or empty");
        }

        if (dto.oldPassword() == null || dto.oldPassword().isEmpty()) {
            throw new ApiRequestException("Old password is required");
        }

        if (dto.newPassword() == null || dto.newPassword().isEmpty()) {
            throw new ApiRequestException("New password is required");
        }

        try {
            userService.updatePassword(dto.username(), dto.oldPassword(), dto.newPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (BadCredentialsException e) {
            throw new ApiRequestException(e.getMessage());
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during password reset");
        }
    }

    @GetMapping("/.well-known/jwks.json")
    public String getJwkSet() {
        return new JWKSet(tokenService.getJWTUtil().getRSAKey()).toJSONObject().toString();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshAccessToken(
            @CookieValue(value="refreshToken", required = false) String tokenId) {
        try {
            RefreshToken refreshToken = tokenService.validateRefreshToken(
                    UUID.fromString(tokenId)
            );

            if (refreshToken == null) {
                throw new InvalidTokenException("Invalid refresh token");
            }

            User user = refreshToken.getUser();
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    user.getUsername()
            );
            UserDTO userDTO = UserMapper.INSTANCE.userToUserDTO(user);

            String accessToken = tokenService.createAccessToken(userDetails);

            ResponseCookie accessTokenCookie = this.buildAccessTokenForCookie(
                    accessToken, accessDurationInSeconds
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .body(new LoginResponseDTO("success", userDTO));
        } catch (InvalidTokenException e) {
            throw new ApiRequestException(e.getMessage());
        } catch (Exception e) {
            throw new ApiRequestException("Error refreshing access token");
        }
    }

    private List<ResponseCookie> buildRefreshTokenForCookie(String attributeValue, long age) {
        List<String> paths = List.of("/api/auth/refresh", "/api/auth/logout");
        List<ResponseCookie> cookies = new ArrayList<>();
        for (String path : paths) {
            cookies.add(ResponseCookie.from("refreshToken", attributeValue)
                    .httpOnly(true)
                    .secure(httpsEnabled)
                    .path(path)  // Define the path that requires cookie sending
                    .maxAge(age)  // Set expiration time
//                    .sameSite("Strict")  // Optional: prevent CSRF on cross-site requests
                    .build()
            );
        }
        return cookies;
    }

    private ResponseCookie buildAccessTokenForCookie(String attributeValue, long age) {
        return ResponseCookie.from("accessToken", attributeValue)
                .httpOnly(true)
                .secure(httpsEnabled)
                .path("/")  // Define the path that requires cookie sending
//                    .sameSite("Strict")  // Optional: prevent CSRF on cross-site requests
                .maxAge(age)
                .build();
    }
}
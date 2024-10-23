package com.cs203.smucode.controllers;

import com.cs203.smucode.dto.JWTResponseDTO;
import com.cs203.smucode.dto.LoginRequestDTO;
import com.cs203.smucode.dto.UserCredentialsDTO;
import com.cs203.smucode.exception.ApiRequestException;
import com.cs203.smucode.mappers.UserMapper;
import com.cs203.smucode.models.User;
import com.cs203.smucode.dto.UserDTO;
import com.cs203.smucode.services.IUserService;
import com.cs203.smucode.utils.JWTUtil;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

/**
 * @author: gav
 * @version: 1.0
 * @since: 2024-10-17
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IUserService userService;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(IUserService userService, JWTUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<JWTResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
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

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDTO userDTO = UserMapper.INSTANCE.userToUserDTO(userService.getUserByUsername(dto.username()));

            return ResponseEntity.ok(new JWTResponseDTO(
                    "success",
                    userDTO, jwtUtil.generateToken(authentication))
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JWTResponseDTO(
                            "Invalid username or password",
                            null, null)
                    );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JWTResponseDTO(
                            "Ensure that you have typed the username and password correctly",
                            null, null)
                    );
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during login", e);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody @Valid UserDTO dto) {
        if (dto.username() == null || dto.username().isEmpty()) {
            throw new ApiRequestException("Username cannot be null or empty");
        }

        if (dto.password() == null || dto.password().isEmpty()) {
            throw new ApiRequestException("Password is required");
        }

        try {
            User createdUser = UserMapper.INSTANCE.userDTOtoUser(dto);
            userService.createUser(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserMapper.INSTANCE.userToUserDTO(createdUser));
        } catch (DataIntegrityViolationException e) {
            throw new ApiRequestException("That username/email already exists, please try another", e);
        } catch (ValidationException e) {
            throw new ApiRequestException(e.getMessage(), e);
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during signup", e);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        //logout logic here?
        return ResponseEntity.ok("User logged out successfully");
    }

    @PostMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(@RequestBody @Valid UserCredentialsDTO dto) {
        if (dto.id() == null) {
            throw new ApiRequestException("UUID cannot be null or empty");
        }

        try {
            userService.deleteUser(dto.id(), dto.oldPassword());
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during delete account", e);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid UserCredentialsDTO dto) {
        if (dto.id() == null) {
            throw new ApiRequestException("UUID cannot be null or empty");
        }

        if (dto.oldPassword() == null || dto.oldPassword().isEmpty()) {
            throw new ApiRequestException("Old password is required");
        }

        if (dto.newPassword() == null || dto.newPassword().isEmpty()) {
            throw new ApiRequestException("New password is required");
        }

        try {
            userService.updatePassword(dto.id(), dto.oldPassword(), dto.newPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during password reset", e);
        }
    }

}
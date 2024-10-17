package com.cs203.smucode.controllers;

import com.cs203.smucode.exception.ApiRequestException;
import com.cs203.smucode.models.User;
import com.cs203.smucode.models.UserDTO;
import jakarta.persistence.PersistenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    @PostMapping("/login")
    public ResponseEntity<JwtUserDTO> login(@RequestBody UserDTO userDTO) {
        try {
            String username = userDTO.username();
            String password = userDTO.password();
            if (username == null || password == null) {
                throw new ApiRequestException("Username and password are required");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDTO.username(), userDTO.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            userDTO = UserMapper.INSTANCE.userToUserDTO(userService.getUserByUsername(username));

            return ResponseEntity.ok(new JwtUserDTO(
                    "success",
                    userDTO, jwtUtil.generateToken(authentication))
            );
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JwtUserDTO(
                            "Invalid username or password",
                            null, null)
                    );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JwtUserDTO(
                            "Ensure that you have typed the username and password correctly",
                            null, null)
                    );
        } catch (ApiRequestException e) {
            logger.info(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new ApiRequestException("An error occurred during login", e);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody UserDTO dto) {
        try {
            if (dto.username() == null || dto.password() == null) {
                throw new ApiRequestException("Username and password are required for signup");
            }
            User createdUser = UserMapper.INSTANCE.userDTOtoUser(dto);
            userService.createUser(createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (PersistenceException e) {
            throw new ApiRequestException("Duplicate username/email, please try another", e);
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiRequestException("An error occurred during signup", e);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        //logout logic here?
        return ResponseEntity.ok("User logged out successfully");
    }
}
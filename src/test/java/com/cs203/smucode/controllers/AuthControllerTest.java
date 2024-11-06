package com.cs203.smucode.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.UUID;

import com.cs203.smucode.dto.UserCredentialsDTO;
import com.cs203.smucode.exception.InvalidTokenException;
import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.services.ITokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import com.cs203.smucode.configs.TestSecurityConfig;
import com.cs203.smucode.dto.LoginRequestDTO;
import com.cs203.smucode.dto.LoginResponseDTO;
import com.cs203.smucode.dto.UserDTO;
import com.cs203.smucode.models.User;
import com.cs203.smucode.models.UserRole;
import com.cs203.smucode.proxies.UserServiceProxy;
import com.cs203.smucode.repositories.UserRepository;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserServiceProxy userServiceProxy;

    @MockBean
    private ITokenService tokenService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        setupTestUser();
        setupMocks();
    }

    private void setupTestUser() {
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("Test123!@"));
        testUser.setEmail("test@example.com");
        testUser.setUserRole(UserRole.PLAYER);
        userRepository.save(testUser);
    }

    private void setupMocks() {
        doNothing().when(userServiceProxy).createUserProfile(any(), any(), any());
        when(tokenService.createAccessToken(any())).thenReturn("dummy-access-token");
        when(tokenService.createRefreshToken(any())).thenReturn("dummy-refresh-token");
    }

    @Nested
    class SignupTests {
        @Test
        void whenValidUser_thenReturnsCreated() throws Exception {
            UserDTO signupRequest = new UserDTO(
                    "newuser",
                    "NewPass123!@#",
                    "newuser@example.com",
                    "PLAYER"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserDTO> request = new HttpEntity<>(signupRequest, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/signup");
            ResponseEntity<UserDTO> response = restTemplate.postForEntity(uri, request, UserDTO.class);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("newuser", response.getBody().username());
        }

        @Test
        void whenDuplicateUsername_thenReturnsBadRequest() throws Exception {
            UserDTO duplicateUsername = new UserDTO("testuser", "Test123!@", "another@example.com", "PLAYER");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/signup");

            ResponseEntity<UserDTO> response = restTemplate.postForEntity(uri, new HttpEntity<>(duplicateUsername, headers), UserDTO.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void whenInvalidCredentials_thenReturnsBadRequest() throws Exception {
            URI uri = new URI("http://localhost:" + port + "/api/auth/signup");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Test invalid password
            UserDTO invalidPassword = new UserDTO("newuser", "weak", "test@example.com", "PLAYER");
            ResponseEntity<UserDTO> response = restTemplate.postForEntity(uri, new HttpEntity<>(invalidPassword, headers), UserDTO.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            // Test invalid email
            UserDTO invalidEmail = new UserDTO("newuser", "Test123!@", "notanemail", "PLAYER");
            response = restTemplate.postForEntity(uri, new HttpEntity<>(invalidEmail, headers), UserDTO.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void whenServiceError_thenReturnsBadRequest() throws Exception {
            UserDTO signupRequest = new UserDTO("newuser", "Test123!@", "newuser@example.com", "PLAYER");

            doThrow(new ResourceAccessException("Service unavailable"))
                    .when(userServiceProxy)
                    .createUserProfile(any(UUID.class), any(String.class), any(String.class));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/signup");

            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(signupRequest, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Something went wrong on our end"));
        }

        @Test
        void whenUnexpectedError_thenReturnsBadRequest() throws Exception {
            UserDTO signupRequest = new UserDTO("newuser", "Test123!@", "newuser@example.com", "PLAYER");
            
            // Mock a generic unexpected error
            doThrow(new RuntimeException("Unexpected error"))
                    .when(userServiceProxy)
                    .createUserProfile(any(), any(), any());
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/signup");
            
            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(signupRequest, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("An error occurred during signup"));
        }
    }

    @Nested
    class LoginTests {
        @Test
        void whenValidCredentials_thenReturnsOk() throws Exception {
            LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "Test123!@");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequest, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/login");
            ResponseEntity<LoginResponseDTO> response = restTemplate.postForEntity(uri, request, LoginResponseDTO.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("success", response.getBody().message());
            assertEquals("testuser", response.getBody().userDTO().username());

            verify(tokenService).createAccessToken(any());
            verify(tokenService).createRefreshToken("testuser");
        }

        @Test
        void whenInvalidCredentials_thenReturnsError() throws Exception {
            URI uri = new URI("http://localhost:" + port + "/api/auth/login");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            LoginRequestDTO nonexistentUser = new LoginRequestDTO("nonexistent", "Test123!@");
            ResponseEntity<LoginResponseDTO> response = restTemplate.postForEntity(uri, new HttpEntity<>(nonexistentUser, headers), LoginResponseDTO.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        void whenEmptyCredentials_thenReturnsBadRequest() throws Exception {
            URI uri = new URI("http://localhost:" + port + "/api/auth/login");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            LoginRequestDTO emptyCredentials = new LoginRequestDTO("", "");
            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(emptyCredentials, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Username cannot be null or empty"));
        }

        @Test
        void whenTokenServiceFails_thenReturnsBadRequest() throws Exception {
            LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "Test123!@");
            
            // Mock token service to throw exception
            when(tokenService.createAccessToken(any())).thenThrow(new RuntimeException("Token creation failed"));
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/login");
            
            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(loginRequest, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("An error occurred during login"));
        }
    }

    @Nested
    class PasswordChangeTests {
        @Test
        void whenValidRequest_thenReturnsOk() throws Exception {
            UserCredentialsDTO request = new UserCredentialsDTO(
                    "testuser",
                    "Test123!@",
                    "NewPass456!@"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserCredentialsDTO> httpRequest = new HttpEntity<>(request, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/change-password");
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, httpRequest, String.class);

            assertEquals("Password changed successfully", response.getBody());
            User updatedUser = userRepository.findByUsername("testuser").orElse(null);
            assertNotNull(updatedUser);
            assertTrue(passwordEncoder.matches("NewPass456!@", updatedUser.getPassword()));
        }

        @Test
        void whenInvalidOldPassword_thenReturnsBadRequest() throws Exception {
            UserCredentialsDTO request = new UserCredentialsDTO("testuser", "WrongPass123!@", "NewPass456!@");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/change-password");

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(request, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Invalid password"));
        }

        @Test
        void whenInvalidNewPassword_thenReturnsBadRequest() throws Exception {
            UserCredentialsDTO request = new UserCredentialsDTO("testuser", "Test123!@", "weak");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/change-password");

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(request, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Password must contain 1 symbol"));
        }

        @Test
        void whenUnexpectedError_thenReturnsBadRequest() throws Exception {
            UserCredentialsDTO request = new UserCredentialsDTO("testuser", "Test123!@", "NewPass456!@");
            
            // Delete user after setup to cause error
            userRepository.deleteAll();
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/change-password");
            
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(request, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("An error occurred during password reset"));
        }
    }

    @Nested
    class AccountDeletionTests {
        @Test
        void whenValidRequest_thenReturnsOk() throws Exception {
            LoginRequestDTO deleteRequest = new LoginRequestDTO("testuser", "Test123!@");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(deleteRequest, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/delete-account");
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("User deleted successfully", response.getBody());
            assertFalse(userRepository.findByUsername("testuser").isPresent());
        }

        @Test
        void whenInvalidCredentials_thenReturnsBadRequest() throws Exception {
            LoginRequestDTO wrongPass = new LoginRequestDTO("testuser", "wrongpassword");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/delete-account");

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, new HttpEntity<>(wrongPass, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void whenUnexpectedError_thenReturnsBadRequest() throws Exception {
            LoginRequestDTO deleteRequest = new LoginRequestDTO("testuser", "Test123!@");

            // Mock proxy to throw exception
            doThrow(new RuntimeException("Unexpected error"))
                    .when(userServiceProxy)
                    .deleteUserProfile(any(), any(), any());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            URI uri = new URI("http://localhost:" + port + "/api/auth/delete-account");

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, new HttpEntity<>(deleteRequest, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("An error occurred during delete account"));
        }
    }

    @Nested
    class SessionManagementTests {
        @Test
        void whenValidLogout_thenReturnsOk() throws Exception {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, "refreshToken=" + UUID.randomUUID().toString());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/logout");
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("User logged out successfully", response.getBody());

            HttpHeaders responseHeaders = response.getHeaders();
            assertTrue(responseHeaders.get(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(cookie -> cookie.contains("refreshToken=destroyedRefresh")));
            assertTrue(responseHeaders.get(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(cookie -> cookie.contains("accessToken=destroyedAccess")));
        }

        @Test
        void whenValidRefreshToken_thenReturnsNewAccessToken() throws Exception {
            UUID mockUuid = UUID.randomUUID();
            User testUser = userRepository.findByUsername("testuser").orElseThrow();
            RefreshToken mockRefreshToken = new RefreshToken();
            mockRefreshToken.setUser(testUser);

            when(tokenService.validateRefreshToken(mockUuid)).thenReturn(mockRefreshToken);
            when(tokenService.createAccessToken(any())).thenReturn("new-access-token");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, "refreshToken=" + mockUuid.toString());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/refresh");
            ResponseEntity<LoginResponseDTO> response = restTemplate.postForEntity(uri, request, LoginResponseDTO.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("success", response.getBody().message());
            assertNotNull(response.getBody().userDTO());
        }

        @Test
        void whenInvalidRefreshToken_thenReturnsBadRequest() throws Exception {
            UUID mockUuid = UUID.randomUUID();
            when(tokenService.validateRefreshToken(mockUuid)).thenThrow(new InvalidTokenException("Invalid refresh token"));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, "refreshToken=" + mockUuid.toString());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);

            URI uri = new URI("http://localhost:" + port + "/api/auth/refresh");
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Invalid refresh token"));
        }

        @Test
        void whenRefreshTokenValidationFails_thenReturnsBadRequest() throws Exception {
            UUID mockUuid = UUID.randomUUID();
            when(tokenService.validateRefreshToken(mockUuid)).thenThrow(new RuntimeException("Unexpected error"));
    
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, "refreshToken=" + mockUuid.toString());
            URI uri = new URI("http://localhost:" + port + "/api/auth/refresh");
            
            ResponseEntity<String> response = restTemplate.postForEntity(uri, new HttpEntity<>(null, headers), String.class);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().contains("Error refreshing access token"));
        }
    }
}

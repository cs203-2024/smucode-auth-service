package com.cs203.smucode.controllers;

import com.cs203.smucode.configs.TestSecurityConfig;
import com.cs203.smucode.dto.*;
import com.cs203.smucode.exception.InvalidTokenException;
import com.cs203.smucode.models.RefreshToken;
import com.cs203.smucode.models.User;
import com.cs203.smucode.models.UserRole;
import com.cs203.smucode.proxies.UserServiceProxy;
import com.cs203.smucode.services.ITokenService;
import com.cs203.smucode.services.IUserService;
import com.cs203.smucode.utils.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private ITokenService tokenService;

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setUserRole(UserRole.PLAYER);

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.createAccessToken(any())).thenReturn("dummy-access-token");
        when(tokenService.createRefreshToken(anyString())).thenReturn(UUID.randomUUID().toString());
    }

    @Nested
    class SignupTests {
        @Test
        void whenValidUser_thenReturnsCreated() throws Exception {
            UserDTO signupRequest = new UserDTO("newuser", "NewPass123!@#", "newuser@example.com", "PLAYER");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("newuser"));

            verify(userService).createUser(any(User.class));
        }

        @Test
        void whenEmptyUsername_thenReturnsBadRequest() throws Exception {
            UserDTO invalidRequest = new UserDTO("", "NewPass123!@#", "newuser@example.com", "PLAYER");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Username cannot be null or empty")));
        }

        @Test
        void whenEmptyPassword_thenReturnsBadRequest() throws Exception {
            UserDTO invalidRequest = new UserDTO("newuser", "", "newuser@example.com", "PLAYER");

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Password must be more than 8 characters")));
        }
    }

    @Nested
    class LoginTests {
        @Test
        void whenValidCredentials_thenReturnsOk() throws Exception {
            LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "Test123!@");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.userDTO.username").value("testuser"))
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"));

            verify(tokenService).createAccessToken(any());
            verify(tokenService).createRefreshToken("testuser");
        }

        @Test
        void whenInvalidCredentials_thenReturnsUnauthorized() throws Exception {
            LoginRequestDTO invalidRequest = new LoginRequestDTO("testuser", "wrongpass");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Ensure that you have typed the username and password correctly"));
        }

        @Test
        void whenEmptyCredentials_thenReturnsBadRequest() throws Exception {
            LoginRequestDTO emptyCredentials = new LoginRequestDTO("", "");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyCredentials)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Username cannot be null or empty")));
        }
    }

    @Nested
    class PasswordChangeTests {
        @Test
        void whenValidRequest_thenReturnsOk() throws Exception {
            PasswordChangeRequestDTO request = new PasswordChangeRequestDTO("testuser", "oldpass", "NewPass@123");

            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Password changed successfully"));

            verify(userService).updatePassword("testuser", "oldpass", "NewPass@123");
        }

        @Test
        void whenEmptyUsername_thenReturnsBadRequest() throws Exception {
            PasswordChangeRequestDTO request = new PasswordChangeRequestDTO(null, "oldpass", "NewPass@123");

            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Username cannot be empty/null")));
        }

        @Test
        void whenEmptyOldPassword_thenReturnsBadRequest() throws Exception {
            PasswordChangeRequestDTO request = new PasswordChangeRequestDTO("testuser", "", "NewPass@123");

            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Old password is required")));
        }

        @Test
        void whenEmptyNewPassword_thenReturnsBadRequest() throws Exception {
            PasswordChangeRequestDTO request = new PasswordChangeRequestDTO("testuser", "oldpass", "");

            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("New Password must contain 1 symbol, 1 uppercase, 1 lowercase and 1 digit")));
        }
    }

    @Nested
    class AccountDeletionTests {
        @Test
        void whenValidRequest_thenReturnsOk() throws Exception {
            DeleteAccountRequestDTO deleteRequest = new DeleteAccountRequestDTO("Test123!@");

            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("testuser");

            mockMvc.perform(delete("/api/auth/delete-account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(deleteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User deleted successfully"));

            verify(userService).deleteUser(anyString(), eq("Test123!@"));
        }

        @Test
        void whenServiceError_thenReturnsBadRequest() throws Exception {
            DeleteAccountRequestDTO deleteRequest = new DeleteAccountRequestDTO("Test123!@");

            doThrow(new RuntimeException("Service error"))
                    .when(userService)
                    .deleteUser(anyString(), anyString());

            mockMvc.perform(delete("/api/auth/delete-account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(deleteRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("An error occurred during delete account")));
        }
    }

    @Nested
    class SessionManagementTests {
        @Test
        void whenValidLogout_thenReturnsOk() throws Exception {
            String refreshTokenId = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/auth/logout")
                            .cookie(new Cookie("refreshToken", refreshTokenId)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User logged out successfully"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().exists("accessToken"));

            verify(tokenService).blacklistRefreshToken(UUID.fromString(refreshTokenId));
        }

        @Test
        void whenValidRefreshToken_thenReturnsNewAccessToken() throws Exception {
            UUID refreshTokenId = UUID.randomUUID();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(testUser);

            when(tokenService.validateRefreshToken(refreshTokenId)).thenReturn(refreshToken);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(tokenService.createAccessToken(any())).thenReturn("new-access-token");

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshTokenId.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(cookie().exists("accessToken"));
        }

        @Test
        void whenInvalidRefreshToken_thenReturnsBadRequest() throws Exception {
            UUID refreshTokenId = UUID.randomUUID();

            when(tokenService.validateRefreshToken(refreshTokenId)).thenReturn(null);

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshTokenId.toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid refresh token")));
        }
    }

    @Test
    void whenGetJwks_thenReturnsJwkSet() throws Exception {
        JWTUtil jwtUtil = mock(JWTUtil.class);
        RSAKey rsaKey = mock(RSAKey.class);
        when(tokenService.getJWTUtil()).thenReturn(jwtUtil);
        when(jwtUtil.getRSAKey()).thenReturn(rsaKey);

        mockMvc.perform(get("/api/auth/.well-known/jwks.json"))
                .andExpect(status().isOk());

        verify(tokenService).getJWTUtil();
        verify(jwtUtil).getRSAKey();
    }
}

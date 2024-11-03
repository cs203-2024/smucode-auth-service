package com.cs203.smucode.services.impl;

import com.cs203.smucode.models.AdminUser;
import com.cs203.smucode.models.PlayerUser;
import com.cs203.smucode.models.User;
import com.cs203.smucode.models.UserRole;
import com.cs203.smucode.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthUserServiceImplTest {
    @Mock
    private IUserService userService;

    @InjectMocks
    private AuthUserServiceImpl authUserService;

    @Test
    void loadUserByUsername_invalidUsername_throwUsernameNotFoundException() {
        // Arrange
        String username = "nonexistentuser";
        when(userService.getUserByUsername(username)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(UsernameNotFoundException.class, () ->
                authUserService.loadUserByUsername(username)
        );

        // Verify
        verify(userService).getUserByUsername(username);
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void loadUserByUsername_validPlayerUser_returnPlayerUserDetails() {
        // Arrange
        String username = "player1";
        User playerUser = new User();
        playerUser.setUsername(username);
        playerUser.setPassword("password");
        playerUser.setUserRole(UserRole.PLAYER);

        when(userService.getUserByUsername(username)).thenReturn(playerUser);

        // Act
        UserDetails result = authUserService.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof PlayerUser);
        assertEquals(username, result.getUsername());
        // Verify
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_validAdminUser_returnAdminUserDetails() {
        // Arrange
        String username = "admin";
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setPassword("password");
        adminUser.setUserRole(UserRole.ADMIN);
        // Stub
        when(userService.getUserByUsername(username)).thenReturn(adminUser);

        // Act
        UserDetails result = authUserService.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof AdminUser);
        assertEquals(username, result.getUsername());
        // Verify
        verify(userService).getUserByUsername(username);
    }
}

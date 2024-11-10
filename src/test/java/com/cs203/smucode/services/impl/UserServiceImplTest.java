package com.cs203.smucode.services.impl;

import com.cs203.smucode.dto.UserIdentificationDTO;
import com.cs203.smucode.models.User;
import com.cs203.smucode.repositories.UserRepository;
import com.cs203.smucode.proxies.UserServiceProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserServiceProxy userServiceProxy;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUserByUsername_validUsername_returnsUser() {
        // Arrange
        String username = "testuser";
        User expectedUser = new User();
        expectedUser.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

        // Act
        User result = userService.getUserByUsername(username);

        // Assert
        assertEquals(username, result.getUsername());

        // Verify
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_invalidUsername_returnsNull() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        User result = userService.getUserByUsername(username);

        // Assert
        assertNull(result);

        // Verify
        verify(userRepository).findByUsername(username);
    }

    @Test
    void createUser_validUser_Success() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("newuser");
        user.setPassword("password");
        user.setEmail("test@test.com");

        // Stub
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.createUser(user);

        // Assert
        assertEquals("encodedPassword", user.getPassword());

        // Verify
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(user);
        verify(userServiceProxy).createUserProfile(any(UserIdentificationDTO.class));
    }

    @Test
    void updatePassword_validCredentials_Success() {
        // Arrange
        String username = "testuser";
        String oldPassword = "oldpass";
        String newPassword = "newpass";

        User user = new User();
        user.setUsername(username);
        user.setPassword("encodedOldPass");

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPass");

        // Act
        userService.updatePassword(username, oldPassword, newPassword);

        // Assert
        assertEquals("encodedNewPass", user.getPassword());

        // Verify
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(oldPassword, "encodedOldPass");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_invalidUsername_throwsException() {
        // Arrange
        String username = "nonexistent";

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userService.updatePassword(username, "oldpass", "newpass"));

        // Verify
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updatePassword_invalidOldPassword_throwsException() {
        // Arrange
        String username = "testuser";
        String oldPassword = "wrongpass";
        User user = new User();
        user.setPassword("encodedPassword");

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> userService.updatePassword(username, oldPassword, "newpass"));

        // Verify
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(oldPassword, user.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_validCredentials_Success() {
        // Arrange
        String username = "testuser";
        String password = "password";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword("encodedPassword");
        user.setEmail("test@test.com");

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        // Act
        userService.deleteUser(username, password);

        // Assert & Verify
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(userServiceProxy).deleteUserProfile(any(UserIdentificationDTO.class));
        verify(userRepository).deleteByUsername(username);
    }

    @Test
    void deleteUser_invalidUsername_ThrowsException() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userService.deleteUser(username, "password"));

        // Verify
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userServiceProxy);
    }

    @Test
    void deleteUser_invalidPassword_throwsException() {
        // Arrange
        String username = "testuser";
        String password = "wrongpass";
        User user = new User();
        user.setPassword("encodedPassword");

        // Stub
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> userService.deleteUser(username, password));
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(userRepository, never()).deleteByUsername(any());
        verifyNoInteractions(userServiceProxy);
    }
}

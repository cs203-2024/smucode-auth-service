package com.cs203.smucode.services.impl;

import com.cs203.smucode.dto.UserIdentificationDTO;
import com.cs203.smucode.proxies.UserServiceProxy;
import com.cs203.smucode.services.IUserService;
import com.cs203.smucode.models.User;
import com.cs203.smucode.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
/**
 * @author: gav
 * @version: 1.0
 * @since: 2024-09-05
 */
@Service
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceProxy userServiceProxy;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserServiceProxy userServiceProxy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userServiceProxy = userServiceProxy;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElse(null);
    }

    @Override
    @Transactional
    public void createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User createdUser = userRepository.save(user);

        // Talk to user service to create a profile for this user
        userServiceProxy.createUserProfile(
                new UserIdentificationDTO(createdUser.getId(),
                        createdUser.getUsername(),
                        createdUser.getEmail()
                )
        );
    }

    @Override
    @Transactional
    public void updatePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override   
    @Transactional
    public void deleteUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid UUID"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
        userServiceProxy.deleteUserProfile(
                new UserIdentificationDTO(user.getId(),
                        user.getUsername(),
                        user.getEmail()
                )
        );
        userRepository.deleteByUsername(username);
    }
}
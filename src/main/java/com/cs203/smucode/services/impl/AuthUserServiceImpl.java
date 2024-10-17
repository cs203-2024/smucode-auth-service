package com.cs203.smucode.services.impl;

import com.cs203.smucode.models.AdminUser;
import com.cs203.smucode.models.PlayerUser;
import com.cs203.smucode.models.User;
import com.cs203.smucode.models.UserRole;
import com.cs203.smucode.services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserServiceImpl implements UserDetailsService {

    private final IUserService userService;

    @Autowired
    public AuthUserServiceImpl(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (user.getUserRole() != UserRole.PLAYER && user.getUserRole() != UserRole.ADMIN) {
            throw new SecurityException("Invalid authority");
        }

        return switch (user.getUserRole()) {
            case PLAYER -> new PlayerUser(user);
            case ADMIN -> new AdminUser(user);
        };
    }
}

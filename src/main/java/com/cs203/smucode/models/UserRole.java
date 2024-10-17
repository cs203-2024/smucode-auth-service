package com.cs203.smucode.models;

import lombok.Getter;

@Getter
public enum UserRole {
    PLAYER("ROLE_PLAYER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }
}

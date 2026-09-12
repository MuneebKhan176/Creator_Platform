package com.application.authentication.dtos;

import com.application.authentication.entities.AccountStatus;
import com.application.authentication.entities.Role;
import com.application.authentication.entities.User;

import java.util.Set;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private boolean emailVerified;
    private AccountStatus accountStatus;
    private Set<Role> roles;

    public UserResponse() {
    }

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.emailVerified = user.isEmailVerified();
        response.accountStatus = user.getAccountStatus();
        response.roles = user.getRoles();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
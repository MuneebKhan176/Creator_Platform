// src/main/java/com/application/profile/dtos/UserSummaryResponse.java
package com.application.profile.dtos;

import com.application.authentication.entities.User;

public class UserSummaryResponse {
    private Long id;
    private String username;

    public static UserSummaryResponse from(User user) {
        UserSummaryResponse dto = new UserSummaryResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
}
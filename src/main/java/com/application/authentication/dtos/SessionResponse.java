package com.application.authentication.dtos;

import com.application.authentication.entities.UserSession;

import java.time.Instant;

public class SessionResponse {

    private Long id;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastUsedAt;
    private boolean current;

    public static SessionResponse from(UserSession session, boolean current) {
        SessionResponse response = new SessionResponse();
        response.id = session.getId();
        response.ipAddress = session.getIpAddress();
        response.userAgent = session.getUserAgent();
        response.createdAt = session.getCreatedAt();
        response.lastUsedAt = session.getLastUsedAt();
        response.current = current;
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public boolean isCurrent() {
        return current;
    }
}
package com.application.authentication.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "unknown" : userAgent;
    }
}
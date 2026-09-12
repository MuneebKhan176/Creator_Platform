package com.application.authentication.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue()).filter(v -> !v.isBlank());
            }
        }
        return Optional.empty();
    }
}
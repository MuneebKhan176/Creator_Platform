package com.application.authentication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Double-submit CSRF check for cookie-authenticated, state-changing
 * endpoints. SameSite=Lax already blocks the auth cookies from attaching to
 * a cross-site POST/DELETE in modern browsers; this is an independent
 * second layer that doesn't rely solely on SameSite enforcement.
 */
public class CsrfValidationFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE_NAME = "csrf_token";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    private static final Set<String> PROTECTED_PREFIXES = Set.of(
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password",
            "/api/v1/auth/sessions"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        if (!requiresCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieValue = CookieUtil.readCookie(request, CSRF_COOKIE_NAME).orElse(null);
        String headerValue = request.getHeader(CSRF_HEADER_NAME);

        if (cookieValue == null || headerValue == null || !cookieValue.equals(headerValue)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"CSRF validation failed.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresCheck(HttpServletRequest request) {
        String method = request.getMethod();
        boolean mutating = "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
        if (!mutating) {
            return false;
        }
        String path = request.getRequestURI();
        return PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
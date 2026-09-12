package com.application.authentication.security;

import com.application.authentication.repositories.UserRepository;
import com.application.authentication.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "access_token";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        CookieUtil.readCookie(request, COOKIE_NAME).ifPresent(token -> {
            try {
                Long userId = jwtUtil.getUserId(token);
                userRepository.findById(userId).ifPresent(user -> {
                    UserPrincipal principal = new UserPrincipal(user);
                    if (principal.isEnabled() && principal.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            } catch (JwtException | IllegalArgumentException e) {
                // Expired/invalid access token: this is expected and routine —
                // it's exactly what /refresh exists to fix. Leave the request
                // anonymous; downstream authorization decides what happens next.
                SecurityContextHolder.clearContext();
            }
        });

        filterChain.doFilter(request, response);
    }
}
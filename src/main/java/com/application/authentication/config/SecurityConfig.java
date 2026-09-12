package com.application.authentication.config;

import com.application.authentication.repositories.UserRepository;
import com.application.authentication.security.JwtAuthenticationFilter;
import com.application.authentication.security.RestAccessDeniedHandler;
import com.application.authentication.security.RestAuthenticationEntryPoint;
import com.application.authentication.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final String allowedOrigin;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin,
                           JwtUtil jwtUtil,
                           UserRepository userRepository,
                           RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler) {
        this.allowedOrigin = allowedOrigin;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtUtil, userRepository);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Auth is via an HttpOnly JWT cookie, not a server session, so
                // there's no session to fixate/CSRF against in the classic sense.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/creator/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers("/api/business/**").hasAnyRole("BUSINESS", "ADMIN")
                        .requestMatchers("/api/user/**")
                                .hasAnyRole("USER", "CREATOR", "BUSINESS", "ADMIN", "MODERATOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
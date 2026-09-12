package com.application.authentication.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.dtos.LoginRequest;
import com.application.authentication.dtos.RegisterRequest;
import com.application.authentication.dtos.UserResponse;
import com.application.authentication.dtos.VerifyEmailRequest;
import com.application.authentication.security.UserPrincipal;
import com.application.authentication.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        UserResponse user = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now log in.", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request,
                                                             HttpServletRequest httpRequest,
                                                             HttpServletResponse response) {
        UserResponse user = authService.login(request, httpRequest, response);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully.", user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserResponse>> refresh(HttpServletRequest request,
                                                               HttpServletResponse response) {
        UserResponse user = authService.refresh(request, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed.", user));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", UserResponse.from(principal.getUser())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal UserPrincipal principal,
                                                         HttpServletResponse response) {
        authService.logoutAll(principal, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out of all devices."));
    }
}
package com.application.authentication.controllers;

import com.application.authentication.dtos.*;
import com.application.authentication.security.UserPrincipal;
import com.application.authentication.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If a pending registration exists for this email, a new verification code has been sent."));
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset. Please log in with your new password."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody ChangePasswordRequest request,
                                                              HttpServletResponse response) {
        authService.changePassword(principal, request, response);
        return ResponseEntity.ok(ApiResponse.success("Password changed. Please log in again."));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> sessions(@AuthenticationPrincipal UserPrincipal principal,
                                                                         HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OK", authService.listSessions(principal, request)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long sessionId,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        authService.revokeSession(principal, sessionId, request, response);
        return ResponseEntity.ok(ApiResponse.success("Session revoked."));
    }

    @PostMapping("/sessions/logout-others")
    public ResponseEntity<ApiResponse<Void>> logoutOthers(@AuthenticationPrincipal UserPrincipal principal,
                                                            HttpServletRequest request) {
        authService.logoutOthers(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Logged out of all other devices."));
    }
}
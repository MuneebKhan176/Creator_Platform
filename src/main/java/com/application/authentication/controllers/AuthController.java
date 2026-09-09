package com.application.authentication.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.dtos.RegisterRequest;
import com.application.authentication.dtos.VerifyEmailRequest;
import com.application.authentication.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
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
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                           HttpServletResponse response) {
        authService.verifyEmail(request, response);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully."));
    }
}

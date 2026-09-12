package com.application.authentication.services;

import com.application.authentication.dtos.LoginRequest;
import com.application.authentication.dtos.RegisterRequest;
import com.application.authentication.dtos.UserResponse;
import com.application.authentication.dtos.VerifyEmailRequest;
import com.application.authentication.entities.AccountStatus;
import com.application.authentication.entities.CreatorProfile;
import com.application.authentication.entities.PendingUser;
import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.authentication.repositories.CreatorProfileRepository;
import com.application.authentication.repositories.PendingUserRepository;
import com.application.authentication.repositories.UserRepository;
import com.application.authentication.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRY_MINUTES = 10;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String AUTH_COOKIE_NAME = "auth_token";

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final boolean cookieSecure;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        PendingUserRepository pendingUserRepository,
                        CreatorProfileRepository creatorProfileRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        JwtUtil jwtUtil,
                        @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.userRepository = userRepository;
        this.pendingUserRepository = pendingUserRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.cookieSecure = cookieSecure;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (username.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (password == null || password.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirm password is required");
        }
        if (!password.equals(confirmPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }

        // If there's already a live (non-expired) pending registration, don't
        // silently overwrite it — surface it so the user knows to check their
        // inbox instead of unknowingly invalidating a code that already went out.
        pendingUserRepository.findByEmail(email).ifPresent(existing -> {
            if (Instant.now().isBefore(existing.getVerificationExpiry())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "A verification code was already sent to this email. Check your inbox, " +
                        "or wait for it to expire before registering again.");
            }
            // Expired and abandoned — safe to clear out and let them start over.
            pendingUserRepository.delete(existing);
        });

        String passwordHash = passwordEncoder.encode(password);
        String code = generateVerificationCode();
        Instant expiry = Instant.now().plus(VERIFICATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        PendingUser pendingUser = new PendingUser(username, email, passwordHash, code, expiry);
        pendingUserRepository.save(pendingUser);

        try {
            emailService.sendVerificationCode(email, code);
        } catch (Exception e) {
            pendingUserRepository.delete(pendingUser);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send verification email. Please try again.");
        }
    }

    /**
     * Activates the account. Deliberately does NOT log the user in — that's a
     * separate, explicit step via /login, per the register -> verify -> login flow.
     */
    @Transactional
    public UserResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalizeEmail(request.getEmail());
        String code = request.getCode() == null ? "" : request.getCode().trim();

        PendingUser pendingUser = pendingUserRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "No pending registration found for this email"));

        if (Instant.now().isAfter(pendingUser.getVerificationExpiry())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification code has expired");
        }
        if (!pendingUser.getVerificationCode().equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }

        User user = new User(pendingUser.getUsername(), pendingUser.getEmail(),
                pendingUser.getPasswordHash(), true);
        user = userRepository.save(user);

        CreatorProfile profile = new CreatorProfile(user, user.getUsername(), "");
        creatorProfileRepository.save(profile);

        pendingUserRepository.delete(pendingUser);

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();

        // Same generic message whether the email doesn't exist or the password is
        // wrong, so a caller can't use login to enumerate registered emails.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been suspended.");
        }
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled.");
        }

        issueAuthCookie(user, response);
        return UserResponse.from(user);
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void issueAuthCookie(User user, HttpServletResponse response) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtUtil.getExpirationMs() / 1000)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateVerificationCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", number);
    }
}
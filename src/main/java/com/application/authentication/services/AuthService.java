package com.application.authentication.services;

import com.application.authentication.dtos.*;
import com.application.authentication.entities.AccountStatus;
import com.application.authentication.entities.CreatorProfile;
import com.application.authentication.entities.PasswordResetToken;
import com.application.authentication.entities.PendingUser;
import com.application.authentication.entities.User;
import com.application.authentication.entities.UserSession;
import com.application.authentication.exceptions.ApiException;
import com.application.authentication.repositories.CreatorProfileRepository;
import com.application.authentication.repositories.PasswordResetTokenRepository;
import com.application.authentication.repositories.PendingUserRepository;
import com.application.authentication.repositories.UserRepository;
import com.application.authentication.repositories.UserSessionRepository;
import com.application.authentication.security.CookieUtil;
import com.application.authentication.security.UserPrincipal;
import com.application.authentication.utils.JwtUtil;
import com.application.authentication.utils.RefreshTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRY_MINUTES = 10;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenUtil refreshTokenUtil;
    private final boolean cookieSecure;
    private final long refreshTokenExpirationMs;
    private final long passwordResetExpirationMs;
    private final String resetPasswordBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        PendingUserRepository pendingUserRepository,
                        CreatorProfileRepository creatorProfileRepository,
                        UserSessionRepository userSessionRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        JwtUtil jwtUtil,
                        RefreshTokenUtil refreshTokenUtil,
                        @Value("${app.cookie.secure}") boolean cookieSecure,
                        @Value("${app.refresh-token.expiration-ms}") long refreshTokenExpirationMs,
                        @Value("${app.password-reset.expiration-ms}") long passwordResetExpirationMs,
                        @Value("${app.frontend.reset-password-url}") String resetPasswordBaseUrl) {
        this.userRepository = userRepository;
        this.pendingUserRepository = pendingUserRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenUtil = refreshTokenUtil;
        this.cookieSecure = cookieSecure;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.passwordResetExpirationMs = passwordResetExpirationMs;
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    // ---------------------------------------------------------------
    // Registration / verification
    // ---------------------------------------------------------------

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

        pendingUserRepository.findByEmail(email).ifPresent(existing -> {
            if (Instant.now().isBefore(existing.getVerificationExpiry())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "A verification code was already sent to this email. Check your inbox, " +
                        "or wait for it to expire before registering again.");
            }
            pendingUserRepository.delete(existing);
        });

        String passwordHash = passwordEncoder.encode(password);
        String code = generateVerificationCode();
        Instant expiry = Instant.now().plus(VERIFICATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        PendingUser pendingUser = new PendingUser(username, email, passwordHash,
                refreshTokenUtil.hash(code), expiry);
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
            pendingUserRepository.delete(pendingUser);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Verification code has expired. Please request a new one.");
        }

        if (pendingUser.getAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            pendingUserRepository.delete(pendingUser);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Too many incorrect attempts. Please request a new verification code.");
        }

        if (!pendingUser.getVerificationCodeHash().equals(refreshTokenUtil.hash(code))) {
            pendingUser.setAttempts(pendingUser.getAttempts() + 1);
            int remaining = MAX_VERIFICATION_ATTEMPTS - pendingUser.getAttempts();
            if (remaining <= 0) {
                pendingUserRepository.delete(pendingUser);
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Too many incorrect attempts. Please request a new verification code.");
            }
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        User user = new User(pendingUser.getUsername(), pendingUser.getEmail(),
                pendingUser.getPasswordHash(), true);
        user = userRepository.save(user);

        CreatorProfile profile = new CreatorProfile(user, user.getUsername(), "");
        creatorProfileRepository.save(profile);

        pendingUserRepository.delete(pendingUser);

        return UserResponse.from(user);
    }

    /**
     * Always responds the same way whether or not a pending registration
     * exists for this email — this endpoint must not be usable to probe
     * which emails are mid-registration.
     */
    @Transactional
    public void resendVerification(EmailRequest request) {
        String email = normalizeEmail(request.getEmail());

        pendingUserRepository.findByEmail(email).ifPresent(pendingUser -> {
            String code = generateVerificationCode();
            pendingUser.setVerificationCodeHash(refreshTokenUtil.hash(code));
            pendingUser.setVerificationExpiry(Instant.now().plus(VERIFICATION_EXPIRY_MINUTES, ChronoUnit.MINUTES));
            pendingUser.setAttempts(0);

            try {
                emailService.sendVerificationCode(email, code);
            } catch (Exception e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to send verification email. Please try again.");
            }
        });
    }

    // ---------------------------------------------------------------
    // Login / refresh / logout
    // ---------------------------------------------------------------

    @Transactional
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();

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

        return issueTokens(user, httpRequest, response);
    }

    @Transactional
    public UserResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = CookieUtil.readCookie(request, REFRESH_COOKIE_NAME)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        String tokenHash = refreshTokenUtil.hash(rawToken);
        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again."));

        if (session.getRevokedAt() != null) {
            Long userId = session.getUser().getId();
            Instant now = Instant.now();
            userSessionRepository.findByUserIdAndRevokedAtIsNull(userId)
                    .forEach(s -> s.setRevokedAt(now));

            clearAccessTokenCookie(response);
            clearRefreshTokenCookie(response);
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Security alert: this session was already used elsewhere. All sessions have been signed out — please log in again.");
        }

        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        }

        User user = session.getUser();
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is no longer active.");
        }

        session.setLastUsedAt(Instant.now());
        session.setRevokedAt(Instant.now());

        return issueTokens(user, request, response);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.readCookie(request, REFRESH_COOKIE_NAME).ifPresent(rawToken -> {
            String tokenHash = refreshTokenUtil.hash(rawToken);
            userSessionRepository.findByRefreshTokenHash(tokenHash)
                    .filter(session -> session.getRevokedAt() == null)
                    .ifPresent(session -> session.setRevokedAt(Instant.now()));
        });

        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    @Transactional
    public void logoutAll(UserPrincipal principal, HttpServletResponse response) {
        Instant now = Instant.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNull(principal.getId())
                .forEach(session -> session.setRevokedAt(now));

        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    // ---------------------------------------------------------------
    // Password recovery / change
    // ---------------------------------------------------------------

    @Transactional
    public void forgotPassword(EmailRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = refreshTokenUtil.generateToken();
            String tokenHash = refreshTokenUtil.hash(rawToken);
            Instant expiresAt = Instant.now().plusMillis(passwordResetExpirationMs);

            passwordResetTokenRepository.save(new PasswordResetToken(user, tokenHash, expiresAt));

            String link = resetPasswordBaseUrl + "?token=" + rawToken;
            try {
                emailService.sendPasswordResetEmail(email, link);
            } catch (Exception e) {
                // Don't let a mail-sending failure leak whether the email
                // exists — log it server-side and let the generic response stand.
                log.error("Failed to send password reset email", e);
            }
        });
        // Same response either way — handled by the controller.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken() == null ? "" : request.getToken().trim();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (rawToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirm password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        String tokenHash = refreshTokenUtil.hash(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or has already been used."));

        if (resetToken.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has already been used.");
        }
        if (Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(Instant.now());

        // A password reset invalidates every existing session — if the reset
        // was needed because the account was compromised, this locks out
        // whoever else was logged in.
        Instant now = Instant.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(session -> session.setRevokedAt(now));
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request, HttpServletResponse response) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        String currentPassword = request.getCurrentPassword() == null ? "" : request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirm password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New passwords do not match");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (newPassword.equals(currentPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "New password must be different from your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Same reasoning as resetPassword(): force every device, including
        // this one, to log in again with the new password.
        Instant now = Instant.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(session -> session.setRevokedAt(now));

        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    // ---------------------------------------------------------------
    // Session / device management
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UserPrincipal principal, HttpServletRequest request) {
        String currentHash = CookieUtil.readCookie(request, REFRESH_COOKIE_NAME)
                .map(refreshTokenUtil::hash)
                .orElse(null);

        return userSessionRepository.findByUserIdAndRevokedAtIsNull(principal.getId()).stream()
                .sorted(Comparator.comparing(UserSession::getLastUsedAt).reversed())
                .map(session -> SessionResponse.from(session, session.getRefreshTokenHash().equals(currentHash)))
                .toList();
    }

    @Transactional
    public void revokeSession(UserPrincipal principal, Long sessionId,
                               HttpServletRequest request, HttpServletResponse response) {
        UserSession session = userSessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(principal.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
        }

        boolean isCurrentSession = CookieUtil.readCookie(request, REFRESH_COOKIE_NAME)
                .map(refreshTokenUtil::hash)
                .map(hash -> hash.equals(session.getRefreshTokenHash()))
                .orElse(false);

        if (isCurrentSession) {
            clearAccessTokenCookie(response);
            clearRefreshTokenCookie(response);
        }
    }

    @Transactional
    public void logoutOthers(UserPrincipal principal, HttpServletRequest request) {
        String currentHash = CookieUtil.readCookie(request, REFRESH_COOKIE_NAME)
                .map(refreshTokenUtil::hash)
                .orElse(null);

        Instant now = Instant.now();
        userSessionRepository.findByUserIdAndRevokedAtIsNull(principal.getId()).stream()
                .filter(session -> !session.getRefreshTokenHash().equals(currentHash))
                .forEach(session -> session.setRevokedAt(now));
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private UserResponse issueTokens(User user, HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenUtil.generateToken();
        String refreshTokenHash = refreshTokenUtil.hash(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        UserSession session = new UserSession(user, refreshTokenHash, expiresAt,
                clientIp(request), clientUserAgent(request));
        userSessionRepository.save(session);

        setAccessTokenCookie(user, response);
        setRefreshTokenCookie(rawRefreshToken, response);

        return UserResponse.from(user);
    }

    private void setAccessTokenCookie(User user, HttpServletResponse response) {
        String token = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtUtil.getAccessTokenExpirationMs() / 1000)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void setRefreshTokenCookie(String rawToken, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenExpirationMs / 1000)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String clientUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "unknown" : userAgent;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateVerificationCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", number);
    }
}
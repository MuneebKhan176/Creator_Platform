package com.application.authentication.services;

import com.application.authentication.dtos.LoginRequest;
import com.application.authentication.dtos.RegisterRequest;
import com.application.authentication.dtos.UserResponse;
import com.application.authentication.dtos.VerifyEmailRequest;
import com.application.authentication.entities.AccountStatus;
import com.application.authentication.entities.CreatorProfile;
import com.application.authentication.entities.PendingUser;
import com.application.authentication.entities.User;
import com.application.authentication.entities.UserSession;
import com.application.authentication.exceptions.ApiException;
import com.application.authentication.repositories.CreatorProfileRepository;
import com.application.authentication.repositories.PendingUserRepository;
import com.application.authentication.repositories.UserRepository;
import com.application.authentication.repositories.UserSessionRepository;
import com.application.authentication.security.CookieUtil;
import com.application.authentication.security.UserPrincipal;
import com.application.authentication.utils.JwtUtil;
import com.application.authentication.utils.RefreshTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenUtil refreshTokenUtil;
    private final boolean cookieSecure;
    private final long refreshTokenExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        PendingUserRepository pendingUserRepository,
                        CreatorProfileRepository creatorProfileRepository,
                        UserSessionRepository userSessionRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        JwtUtil jwtUtil,
                        RefreshTokenUtil refreshTokenUtil,
                        @Value("${app.cookie.secure}") boolean cookieSecure,
                        @Value("${app.refresh-token.expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.pendingUserRepository = pendingUserRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenUtil = refreshTokenUtil;
        this.cookieSecure = cookieSecure;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
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

    @Transactional
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
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

        return issueTokens(user, httpRequest, response);
    }

    /**
     * Exchanges a valid, unexpired refresh token for a new access token, and
     * rotates the refresh token itself (old one is revoked, a new one is issued).
     */
    @Transactional
    public UserResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = CookieUtil.readCookie(request, REFRESH_COOKIE_NAME)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        String tokenHash = refreshTokenUtil.hash(rawToken);
        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again."));

        if (session.getRevokedAt() != null) {
            // This exact refresh token was already rotated away (or revoked via
            // logout). Seeing it presented again means it was copied and reused
            // after that point — treat it as a compromised session and kill
            // every active session for this user as a precaution.
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

    /**
     * Creates a new session row plus a fresh access-token cookie and
     * refresh-token cookie. Shared by login() and refresh() (rotation).
     */
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
                // Scoped to just the auth endpoints that need it, rather than "/"
                // like the access token — no reason for it to go out on every request.
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
package com.application.authentication.services;

import com.application.authentication.repositories.PasswordResetTokenRepository;
import com.application.authentication.repositories.PendingUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PendingUserCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PendingUserCleanupService.class);

    private final PendingUserRepository pendingUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PendingUserCleanupService(PendingUserRepository pendingUserRepository,
                                      PasswordResetTokenRepository passwordResetTokenRepository) {
        this.pendingUserRepository = pendingUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Scheduled(fixedRate = 60_000)
    public void deleteExpiredPendingUsers() {
        try {
            pendingUserRepository.deleteByVerificationExpiryBefore(Instant.now());
        } catch (Exception e) {
            log.error("Failed to clean up expired pending users", e);
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void deleteExpiredPasswordResetTokens() {
        try {
            passwordResetTokenRepository.deleteByExpiresAtBefore(Instant.now());
        } catch (Exception e) {
            log.error("Failed to clean up expired password reset tokens", e);
        }
    }
}
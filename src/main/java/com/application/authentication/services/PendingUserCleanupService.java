package com.application.authentication.services;

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

    public PendingUserCleanupService(PendingUserRepository pendingUserRepository) {
        this.pendingUserRepository = pendingUserRepository;
    }

    // Runs once a minute. Catches pending registrations the user simply
    // abandoned (never re-registered, never verified) that the check inside
    // AuthService.register() would otherwise never touch.
    @Scheduled(fixedRate = 60_000)
    public void deleteExpiredPendingUsers() {
        try {
            pendingUserRepository.deleteByVerificationExpiryBefore(Instant.now());
        } catch (Exception e) {
            // A failed cleanup run should never crash the scheduler thread or
            // take down the app — just log it and try again next minute.
            log.error("Failed to clean up expired pending users", e);
        }
    }
}
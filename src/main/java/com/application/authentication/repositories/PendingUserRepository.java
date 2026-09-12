package com.application.authentication.repositories;

import com.application.authentication.entities.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {

    Optional<PendingUser> findByEmail(String email);

    void deleteByEmail(String email);

    // Bulk delete needs @Modifying (and a transaction) — Spring Data won't run
    // an UPDATE/DELETE-style derived query without it.
    @Modifying
    @Transactional
    void deleteByVerificationExpiryBefore(Instant cutoff);
}
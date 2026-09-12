package com.application.authentication.repositories;

import com.application.authentication.entities.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedAtIsNull(Long userId);
}
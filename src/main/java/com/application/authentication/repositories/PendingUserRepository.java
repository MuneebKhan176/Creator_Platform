package com.application.authentication.repositories;

import com.application.authentication.entities.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {

    Optional<PendingUser> findByEmail(String email);

    void deleteByEmail(String email);
}

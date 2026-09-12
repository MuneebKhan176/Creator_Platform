package com.application.authentication.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pending_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_users_email", columnNames = "email")
})
public class PendingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Holds a SHA-256 hash of the 6-digit code, never the plaintext code.
    @Column(name = "verification_code", nullable = false, length = 64)
    private String verificationCodeHash;

    @Column(name = "verification_expiry", nullable = false)
    private Instant verificationExpiry;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PendingUser() {
    }

    public PendingUser(String username, String email, String passwordHash,
                        String verificationCodeHash, Instant verificationExpiry) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.verificationCodeHash = verificationCodeHash;
        this.verificationExpiry = verificationExpiry;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getVerificationCodeHash() {
        return verificationCodeHash;
    }

    public void setVerificationCodeHash(String verificationCodeHash) {
        this.verificationCodeHash = verificationCodeHash;
    }

    public Instant getVerificationExpiry() {
        return verificationExpiry;
    }

    public void setVerificationExpiry(Instant verificationExpiry) {
        this.verificationExpiry = verificationExpiry;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
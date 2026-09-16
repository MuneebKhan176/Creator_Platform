// src/main/java/com/application/profile/entities/UserFollow.java
package com.application.profile.entities;

import com.application.authentication.entities.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_follows",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_follows_follower_followee", columnNames = {"follower_id", "followee_id"}),
        indexes = {
                @Index(name = "idx_user_follows_follower_id", columnList = "follower_id"),
                @Index(name = "idx_user_follows_followee_id", columnList = "followee_id")
        })
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user doing the following.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    // The user being followed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserFollow() {
    }

    public UserFollow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }
    public User getFollowee() { return followee; }
    public void setFollowee(User followee) { this.followee = followee; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
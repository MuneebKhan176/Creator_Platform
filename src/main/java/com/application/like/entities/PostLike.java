// src/main/java/com/application/like/entities/PostLike.java
package com.application.like.entities;

import com.application.authentication.entities.User;
import com.application.post.entities.Post;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "post_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_likes_post_user", columnNames = {"post_id", "user_id"}),
        indexes = @Index(name = "idx_post_likes_post_id", columnList = "post_id"))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostLike() {
    }

    public PostLike(Post post, User user) {
        this.post = post;
        this.user = user;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
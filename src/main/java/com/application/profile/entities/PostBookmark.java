// src/main/java/com/application/profile/entities/PostBookmark.java
package com.application.profile.entities;

import com.application.authentication.entities.User;
import com.application.post.entities.Post;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "post_bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_bookmarks_user_post", columnNames = {"user_id", "post_id"}),
        indexes = @Index(name = "idx_post_bookmarks_user_id_created_at", columnList = "user_id, created_at"))
public class PostBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostBookmark() {
    }

    public PostBookmark(User user, Post post) {
        this.user = user;
        this.post = post;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
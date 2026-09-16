// src/main/java/com/application/post/entities/PostMedia.java
package com.application.post.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "post_media", indexes = {
        @Index(name = "idx_post_media_post_id", columnList = "post_id")
})
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private PostMediaType mediaType;

    // Same pattern as CreatorProfile.profilePictureKey — kept separately from
    // the public URL so the R2 object can be cleaned up later if needed.
    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostMedia() {
    }

    public PostMedia(PostMediaType mediaType, String storageKey, String url) {
        this.mediaType = mediaType;
        this.storageKey = storageKey;
        this.url = url;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public PostMediaType getMediaType() { return mediaType; }
    public void setMediaType(PostMediaType mediaType) { this.mediaType = mediaType; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
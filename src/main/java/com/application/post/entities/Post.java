// src/main/java/com/application/post/entities/Post.java
package com.application.post.entities;

import com.application.authentication.entities.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_created_at", columnList = "created_at DESC")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Nullable: a post can be media-only.
    @Column(name = "content", length = 5000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<PostMedia> media = new ArrayList<>();

    public Post() {
    }

    public Post(User author, String content) {
        this.author = author;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public void addMedia(PostMedia item) {
        item.setPost(this);
        media.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<PostMedia> getMedia() { return media; }
    public void setMedia(List<PostMedia> media) { this.media = media; }
}
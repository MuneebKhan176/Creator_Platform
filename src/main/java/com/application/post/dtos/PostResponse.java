// src/main/java/com/application/post/dtos/PostResponse.java — added likeCount + likedByCurrentUser
package com.application.post.dtos;

import com.application.post.entities.Post;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class PostResponse {
    private Long id;
    private Long authorId;
    private String authorUsername;
    private String content;
    private Instant createdAt;
    private List<PostMediaResponse> media;
    private long commentCount;
    private long likeCount;              // NEW
    private boolean likedByCurrentUser;   // NEW

    public static PostResponse from(Post post) {
        PostResponse dto = new PostResponse();
        dto.id = post.getId();
        dto.authorId = post.getAuthor().getId();
        dto.authorUsername = post.getAuthor().getUsername();
        dto.content = post.getContent();
        dto.createdAt = post.getCreatedAt();
        dto.media = post.getMedia().stream().map(PostMediaResponse::from).collect(Collectors.toList());
        dto.commentCount = 0;
        dto.likeCount = 0;
        dto.likedByCurrentUser = false;
        return dto;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PostMediaResponse> getMedia() { return media; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
}
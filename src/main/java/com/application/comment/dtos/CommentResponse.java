// src/main/java/com/application/comment/dtos/CommentResponse.java
package com.application.comment.dtos;

import com.application.comment.entities.Comment;

import java.time.Instant;

public class CommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private String content;
    private Instant createdAt;

    public static CommentResponse from(Comment comment) {
        CommentResponse dto = new CommentResponse();
        dto.id = comment.getId();
        dto.postId = comment.getPost().getId();
        dto.authorId = comment.getAuthor().getId();
        dto.authorUsername = comment.getAuthor().getUsername();
        dto.content = comment.getContent();
        dto.createdAt = comment.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
// src/main/java/com/application/post/dtos/PostMediaResponse.java
package com.application.post.dtos;

import com.application.post.entities.PostMedia;
import com.application.post.entities.PostMediaType;

import java.time.Instant;

public class PostMediaResponse {
    private Long id;
    private PostMediaType mediaType;
    private String url;
    private Instant createdAt;

    public static PostMediaResponse from(PostMedia media) {
        PostMediaResponse dto = new PostMediaResponse();
        dto.id = media.getId();
        dto.mediaType = media.getMediaType();
        dto.url = media.getUrl();
        dto.createdAt = media.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public PostMediaType getMediaType() { return mediaType; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }
}
// src/main/java/com/application/profile/dtos/BookmarkedPostResponse.java
package com.application.profile.dtos;

import com.application.post.dtos.PostResponse;

import java.time.Instant;

public class BookmarkedPostResponse {
    private PostResponse post;
    private Instant bookmarkedAt;

    public BookmarkedPostResponse(PostResponse post, Instant bookmarkedAt) {
        this.post = post;
        this.bookmarkedAt = bookmarkedAt;
    }

    public PostResponse getPost() { return post; }
    public Instant getBookmarkedAt() { return bookmarkedAt; }
}
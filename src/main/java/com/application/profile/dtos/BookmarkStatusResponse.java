// src/main/java/com/application/profile/dtos/BookmarkStatusResponse.java
package com.application.profile.dtos;

public class BookmarkStatusResponse {
    private Long postId;
    private boolean bookmarked;

    public BookmarkStatusResponse(Long postId, boolean bookmarked) {
        this.postId = postId;
        this.bookmarked = bookmarked;
    }

    public Long getPostId() { return postId; }
    public boolean isBookmarked() { return bookmarked; }
}
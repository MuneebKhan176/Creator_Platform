// src/main/java/com/application/profile/dtos/BookmarkListResponse.java
package com.application.profile.dtos;

import java.util.List;

public class BookmarkListResponse {
    private List<BookmarkedPostResponse> bookmarks;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;

    public BookmarkListResponse(List<BookmarkedPostResponse> bookmarks, int page, int size, long totalElements, boolean hasMore) {
        this.bookmarks = bookmarks;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
    }

    public List<BookmarkedPostResponse> getBookmarks() { return bookmarks; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public boolean isHasMore() { return hasMore; }
}
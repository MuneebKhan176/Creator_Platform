// src/main/java/com/application/post/dtos/FeedResponse.java
package com.application.post.dtos;

import java.util.List;

// Wrapping the list now (instead of returning a bare array) means pagination
// metadata can be consumed by the frontend later without an endpoint change.
public class FeedResponse {
    private List<PostResponse> posts;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;

    public FeedResponse(List<PostResponse> posts, int page, int size, long totalElements, boolean hasMore) {
        this.posts = posts;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
    }

    public List<PostResponse> getPosts() { return posts; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public boolean isHasMore() { return hasMore; }
}
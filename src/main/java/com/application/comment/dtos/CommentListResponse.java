// src/main/java/com/application/comment/dtos/CommentListResponse.java
package com.application.comment.dtos;

import java.util.List;

public class CommentListResponse {
    private List<CommentResponse> comments;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;

    public CommentListResponse(List<CommentResponse> comments, int page, int size, long totalElements, boolean hasMore) {
        this.comments = comments;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
    }

    public List<CommentResponse> getComments() { return comments; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public boolean isHasMore() { return hasMore; }
}
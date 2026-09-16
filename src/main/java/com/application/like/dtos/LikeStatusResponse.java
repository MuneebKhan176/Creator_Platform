// src/main/java/com/application/like/dtos/LikeStatusResponse.java
package com.application.like.dtos;

public class LikeStatusResponse {
    private Long postId;
    private boolean liked;
    private long likeCount;

    public LikeStatusResponse(Long postId, boolean liked, long likeCount) {
        this.postId = postId;
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public Long getPostId() { return postId; }
    public boolean isLiked() { return liked; }
    public long getLikeCount() { return likeCount; }
}
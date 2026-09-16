// src/main/java/com/application/profile/dtos/FollowStatusResponse.java
package com.application.profile.dtos;

public class FollowStatusResponse {
    private Long userId;
    private boolean following;
    private long followerCount;
    private long followingCount;

    public FollowStatusResponse(Long userId, boolean following, long followerCount, long followingCount) {
        this.userId = userId;
        this.following = following;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
    }

    public Long getUserId() { return userId; }
    public boolean isFollowing() { return following; }
    public long getFollowerCount() { return followerCount; }
    public long getFollowingCount() { return followingCount; }
}
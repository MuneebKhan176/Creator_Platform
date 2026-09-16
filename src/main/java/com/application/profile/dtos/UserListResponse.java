// src/main/java/com/application/profile/dtos/UserListResponse.java
package com.application.profile.dtos;

import java.util.List;

public class UserListResponse {
    private List<UserSummaryResponse> users;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;

    public UserListResponse(List<UserSummaryResponse> users, int page, int size, long totalElements, boolean hasMore) {
        this.users = users;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
    }

    public List<UserSummaryResponse> getUsers() { return users; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public boolean isHasMore() { return hasMore; }
}
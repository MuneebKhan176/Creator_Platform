// src/main/java/com/application/profile/controllers/FollowController.java
package com.application.profile.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.security.UserPrincipal;
import com.application.profile.dtos.FollowStatusResponse;
import com.application.profile.dtos.UserListResponse;
import com.application.profile.services.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile/{userId}")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> follow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Followed.", followService.follow(principal.getUser(), userId)));
    }

    @DeleteMapping("/follow")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> unfollow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Unfollowed.", followService.unfollow(principal.getUser(), userId)));
    }

    @GetMapping("/follow-status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> status(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("OK", followService.getStatus(principal.getUser(), userId)));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<UserListResponse>> followers(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", followService.listFollowers(userId, page, size)));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<UserListResponse>> following(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", followService.listFollowing(userId, page, size)));
    }
}
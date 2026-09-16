// src/main/java/com/application/like/controllers/PostLikeController.java
package com.application.like.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.security.UserPrincipal;
import com.application.like.dtos.LikeStatusResponse;
import com.application.like.services.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LikeStatusResponse>> like(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Post liked.", postLikeService.like(principal.getUser(), postId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<LikeStatusResponse>> unlike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Post unliked.", postLikeService.unlike(principal.getUser(), postId)));
    }
}
// src/main/java/com/application/profile/controllers/PostBookmarkController.java
package com.application.profile.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.security.UserPrincipal;
import com.application.profile.dtos.BookmarkListResponse;
import com.application.profile.dtos.BookmarkStatusResponse;
import com.application.profile.services.PostBookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile/bookmarks")
public class PostBookmarkController {

    private final PostBookmarkService postBookmarkService;

    public PostBookmarkController(PostBookmarkService postBookmarkService) {
        this.postBookmarkService = postBookmarkService;
    }

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<BookmarkStatusResponse>> bookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Post bookmarked.",
                postBookmarkService.bookmark(principal.getUser(), postId)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<BookmarkStatusResponse>> unbookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Bookmark removed.",
                postBookmarkService.unbookmark(principal.getUser(), postId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BookmarkListResponse>> listBookmarks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                postBookmarkService.listBookmarks(principal.getUser(), page, size)));
    }
}
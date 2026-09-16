// src/main/java/com/application/post/controllers/PostController.java — only getFeed() changed
package com.application.post.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.security.UserPrincipal;
import com.application.post.dtos.FeedResponse;
import com.application.post.dtos.PostResponse;
import com.application.post.services.PostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "media", required = false) List<MultipartFile> media) {
        PostResponse post = postService.createPost(principal.getUser(), content, media);
        return ResponseEntity.ok(ApiResponse.success("Post created.", post));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FeedResponse>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                postService.getFeed(page, size, principal.getUser().getId())));
    }
}
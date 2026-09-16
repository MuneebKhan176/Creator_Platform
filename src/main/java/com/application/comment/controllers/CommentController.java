// src/main/java/com/application/comment/controllers/CommentController.java
package com.application.comment.controllers;

import com.application.authentication.dtos.ApiResponse;
import com.application.authentication.security.UserPrincipal;
import com.application.comment.dtos.CommentListResponse;
import com.application.comment.dtos.CommentResponse;
import com.application.comment.dtos.CreateCommentRequest;
import com.application.comment.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse comment = commentService.addComment(principal.getUser(), postId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success("Comment added.", comment));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CommentListResponse>> getComments(
            @PathVariable Long postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", commentService.getComments(postId, page, size)));
    }
}
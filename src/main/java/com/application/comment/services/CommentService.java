// src/main/java/com/application/comment/services/CommentService.java
package com.application.comment.services;

import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.comment.dtos.CommentListResponse;
import com.application.comment.dtos.CommentResponse;
import com.application.comment.entities.Comment;
import com.application.comment.repositories.CommentRepository;
import com.application.post.entities.Post;
import com.application.post.repositories.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public CommentResponse addComment(User author, Long postId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));

        // @Valid/@NotBlank on the controller DTO already rejects blank/too-long
        // content before this runs — trim here just to strip stray whitespace.
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty.");
        }

        Comment comment = new Comment(post, author, trimmed);
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public CommentListResponse getComments(Long postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Post not found");
        }

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Page<Comment> result = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(safePage, safeSize));
        List<CommentResponse> comments = result.getContent().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return new CommentListResponse(comments, safePage, safeSize, result.getTotalElements(), result.hasNext());
    }
}
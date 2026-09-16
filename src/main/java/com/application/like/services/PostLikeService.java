// src/main/java/com/application/like/services/PostLikeService.java
package com.application.like.services;

import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.like.dtos.LikeStatusResponse;
import com.application.like.entities.PostLike;
import com.application.like.repositories.PostLikeRepository;
import com.application.post.entities.Post;
import com.application.post.repositories.PostRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    public PostLikeService(PostLikeRepository postLikeRepository, PostRepository postRepository) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public LikeStatusResponse like(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));

        if (!postLikeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            try {
                postLikeRepository.save(new PostLike(post, user));
            } catch (DataIntegrityViolationException e) {
                // Race: two concurrent like requests for the same (post, user)
                // pair both passed the exists() check; the DB unique
                // constraint rejected the second insert. End state is still
                // "liked", so treat this as success rather than an error.
            }
        }

        return new LikeStatusResponse(postId, true, postLikeRepository.countByPostId(postId));
    }

    @Transactional
    public LikeStatusResponse unlike(User user, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Post not found");
        }

        postLikeRepository.deleteByPostIdAndUserId(postId, user.getId());
        return new LikeStatusResponse(postId, false, postLikeRepository.countByPostId(postId));
    }
}
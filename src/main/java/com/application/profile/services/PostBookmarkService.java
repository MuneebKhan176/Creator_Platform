// src/main/java/com/application/profile/services/PostBookmarkService.java
package com.application.profile.services;

import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.post.dtos.PostResponse;
import com.application.post.entities.Post;
import com.application.post.repositories.PostRepository;
import com.application.post.services.PostService;
import com.application.profile.dtos.BookmarkListResponse;
import com.application.profile.dtos.BookmarkStatusResponse;
import com.application.profile.dtos.BookmarkedPostResponse;
import com.application.profile.entities.PostBookmark;
import com.application.profile.repositories.PostBookmarkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostBookmarkService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    public PostBookmarkService(PostBookmarkRepository postBookmarkRepository,
                                PostRepository postRepository,
                                PostService postService) {
        this.postBookmarkRepository = postBookmarkRepository;
        this.postRepository = postRepository;
        this.postService = postService;
    }

    @Transactional
    public BookmarkStatusResponse bookmark(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));

        if (!postBookmarkRepository.existsByPostIdAndUserId(postId, user.getId())) {
            try {
                postBookmarkRepository.save(new PostBookmark(user, post));
            } catch (DataIntegrityViolationException e) {
                // Race: same idempotency handling as PostLikeService — end
                // state is still "bookmarked", not an error.
            }
        }
        return new BookmarkStatusResponse(postId, true);
    }

    @Transactional
    public BookmarkStatusResponse unbookmark(User user, Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Post not found");
        }
        postBookmarkRepository.deleteByPostIdAndUserId(postId, user.getId());
        return new BookmarkStatusResponse(postId, false);
    }

    @Transactional(readOnly = true)
    public BookmarkListResponse listBookmarks(User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Page<PostBookmark> result = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(safePage, safeSize));

        List<PostBookmark> bookmarks = result.getContent();
        List<PostResponse> postResponses = bookmarks.stream()
                .map(b -> PostResponse.from(b.getPost()))
                .collect(Collectors.toList());

        // Reuses PostService's batched comment/like enrichment instead of
        // duplicating that query logic here.
        postService.attachEngagement(postResponses, user.getId());

        List<BookmarkedPostResponse> items = java.util.stream.IntStream.range(0, bookmarks.size())
                .mapToObj(i -> new BookmarkedPostResponse(postResponses.get(i), bookmarks.get(i).getCreatedAt()))
                .collect(Collectors.toList());

        return new BookmarkListResponse(items, safePage, safeSize, result.getTotalElements(), result.hasNext());
    }
}
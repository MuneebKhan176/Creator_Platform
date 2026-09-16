// src/main/java/com/application/post/services/PostService.java — only getFeed() split out; createPost() unchanged
package com.application.post.services;

import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.comment.repositories.CommentRepository;
import com.application.like.repositories.PostLikeRepository;
import com.application.post.dtos.FeedResponse;
import com.application.post.dtos.PostResponse;
import com.application.post.entities.Post;
import com.application.post.entities.PostMedia;
import com.application.post.entities.PostMediaType;
import com.application.post.repositories.PostRepository;
import com.application.profile.services.CloudStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");

    private static final long MAX_IMAGE_SIZE_BYTES = 8L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 50L * 1024 * 1024;

    private static final int MAX_IMAGES_PER_POST = 10;
    private static final int MAX_VIDEOS_PER_POST = 1;

    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_FEED_PAGE_SIZE = 100;
    private static final int DEFAULT_FEED_PAGE_SIZE = 50;

    private static final String STORAGE_FOLDER = "posts";

    private final PostRepository postRepository;
    private final CloudStorageService cloudStorageService;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(PostRepository postRepository, CloudStorageService cloudStorageService,
                        CommentRepository commentRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.cloudStorageService = cloudStorageService;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
    }

    @Transactional
    public PostResponse createPost(User author, String content, List<MultipartFile> media) {
        String trimmed = content == null ? null : content.trim();
        String finalContent = (trimmed == null || trimmed.isBlank()) ? null : trimmed;

        List<MultipartFile> files = media == null
                ? List.of()
                : media.stream().filter(f -> f != null && !f.isEmpty()).collect(Collectors.toList());

        if (finalContent == null && files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A post needs text content or at least one image/video.");
        }
        if (finalContent != null && finalContent.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Post content must be " + MAX_CONTENT_LENGTH + " characters or fewer.");
        }

        int imageCount = 0;
        int videoCount = 0;
        for (MultipartFile file : files) {
            if (resolveMediaType(file.getContentType()) == PostMediaType.IMAGE) {
                imageCount++;
            } else {
                videoCount++;
            }
        }
        if (imageCount > MAX_IMAGES_PER_POST) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A post can include at most " + MAX_IMAGES_PER_POST + " images.");
        }
        if (videoCount > MAX_VIDEOS_PER_POST) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A post can include at most " + MAX_VIDEOS_PER_POST + " video.");
        }

        Post post = new Post(author, finalContent);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                PostMediaType type = resolveMediaType(file.getContentType());
                Set<String> allowedTypes = type == PostMediaType.IMAGE ? ALLOWED_IMAGE_TYPES : ALLOWED_VIDEO_TYPES;
                long maxSize = type == PostMediaType.IMAGE ? MAX_IMAGE_SIZE_BYTES : MAX_VIDEO_SIZE_BYTES;

                CloudStorageService.UploadedFile uploaded =
                        cloudStorageService.upload(file, STORAGE_FOLDER, allowedTypes, maxSize);
                uploadedKeys.add(uploaded.key());
                post.addMedia(new PostMedia(type, uploaded.key(), uploaded.url()));
            }
        } catch (RuntimeException e) {
            for (String key : uploadedKeys) {
                cloudStorageService.delete(key);
            }
            throw e;
        }

        return PostResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(int page, int size, Long currentUserId) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_FEED_PAGE_SIZE : Math.min(size, MAX_FEED_PAGE_SIZE);

        Page<Post> result = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));

        List<PostResponse> posts = result.getContent().stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        attachEngagement(posts, currentUserId);

        return new FeedResponse(posts, safePage, safeSize, result.getTotalElements(), result.hasNext());
    }

    /**
     * Batch-populates commentCount/likeCount/likedByCurrentUser on an
     * arbitrary list of PostResponses (in place). Extracted out of getFeed()
     * so other callers — e.g. the bookmarks list — get the same counts
     * without duplicating the query logic. Behavior for getFeed() itself is
     * unchanged; this is a pure refactor.
     */
    public void attachEngagement(List<PostResponse> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return;
        }
        List<Long> postIds = posts.stream().map(PostResponse::getId).collect(Collectors.toList());

        Map<Long, Long> commentCounts = commentRepository.countGroupedByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        CommentRepository.PostCommentCount::getPostId,
                        CommentRepository.PostCommentCount::getTotal));

        Map<Long, Long> likeCounts = postLikeRepository.countGroupedByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCount::getPostId,
                        PostLikeRepository.PostLikeCount::getTotal));

        Set<Long> likedPostIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(postLikeRepository.findLikedPostIds(currentUserId, postIds));

        posts.forEach(p -> {
            p.setCommentCount(commentCounts.getOrDefault(p.getId(), 0L));
            p.setLikeCount(likeCounts.getOrDefault(p.getId(), 0L));
            p.setLikedByCurrentUser(likedPostIds.contains(p.getId()));
        });
    }

    private PostMediaType resolveMediaType(String contentType) {
        if (contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return PostMediaType.IMAGE;
        }
        if (contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return PostMediaType.VIDEO;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Unsupported file type" + (contentType != null ? ": " + contentType : "")
                        + ". Allowed: JPEG/PNG/WEBP images or MP4/MOV/WEBM video.");
    }
}
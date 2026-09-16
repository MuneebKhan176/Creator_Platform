// src/main/java/com/application/profile/services/FollowService.java
package com.application.profile.services;

import com.application.authentication.entities.User;
import com.application.authentication.exceptions.ApiException;
import com.application.authentication.repositories.UserRepository;
import com.application.profile.dtos.FollowStatusResponse;
import com.application.profile.dtos.UserListResponse;
import com.application.profile.dtos.UserSummaryResponse;
import com.application.profile.entities.UserFollow;
import com.application.profile.repositories.UserFollowRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    public FollowService(UserFollowRepository userFollowRepository, UserRepository userRepository) {
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FollowStatusResponse follow(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot follow yourself.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!userFollowRepository.existsByFollowerIdAndFolloweeId(currentUser.getId(), targetUserId)) {
            try {
                userFollowRepository.save(new UserFollow(currentUser, target));
            } catch (DataIntegrityViolationException e) {
                // Race: two concurrent follow requests for the same pair both
                // passed the exists() check; unique constraint rejected the
                // second insert. End state is still "following" — not an error.
            }
        }

        return buildStatus(currentUser.getId(), targetUserId);
    }

    @Transactional
    public FollowStatusResponse unfollow(User currentUser, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        userFollowRepository.deleteByFollowerIdAndFolloweeId(currentUser.getId(), targetUserId);
        return buildStatus(currentUser.getId(), targetUserId);
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(User currentUser, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        return buildStatus(currentUser.getId(), targetUserId);
    }

    @Transactional(readOnly = true)
    public UserListResponse listFollowers(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        return toUserListResponse(userFollowRepository.findFollowers(userId, pageable(page, size)), page, size);
    }

    @Transactional(readOnly = true)
    public UserListResponse listFollowing(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        return toUserListResponse(userFollowRepository.findFollowing(userId, pageable(page, size)), page, size);
    }

    private FollowStatusResponse buildStatus(Long currentUserId, Long targetUserId) {
        boolean following = userFollowRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetUserId);
        long followerCount = userFollowRepository.countByFolloweeId(targetUserId);
        long followingCount = userFollowRepository.countByFollowerId(targetUserId);
        return new FollowStatusResponse(targetUserId, following, followerCount, followingCount);
    }

    private UserListResponse toUserListResponse(Page<User> page, int pageNum, int size) {
        List<UserSummaryResponse> users = page.getContent().stream()
                .map(UserSummaryResponse::from)
                .collect(Collectors.toList());
        return new UserListResponse(users, pageNum, size, page.getTotalElements(), page.hasNext());
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
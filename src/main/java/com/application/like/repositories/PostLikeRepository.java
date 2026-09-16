// src/main/java/com/application/like/repositories/PostLikeRepository.java
package com.application.like.repositories;

import com.application.like.entities.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.post.id = :postId and pl.user.id = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    // Batched count for a page of posts — mirrors CommentRepository's pattern
    // so the feed doesn't run one COUNT query per post.
    @Query("select pl.post.id as postId, count(pl) as total from PostLike pl where pl.post.id in :postIds group by pl.post.id")
    List<PostLikeCount> countGroupedByPostIds(@Param("postIds") List<Long> postIds);

    // Which of this page's posts the current user has already liked.
    @Query("select pl.post.id from PostLike pl where pl.user.id = :userId and pl.post.id in :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    interface PostLikeCount {
        Long getPostId();
        Long getTotal();
    }
}
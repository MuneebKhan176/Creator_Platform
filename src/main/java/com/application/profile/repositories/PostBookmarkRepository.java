// src/main/java/com/application/profile/repositories/PostBookmarkRepository.java
package com.application.profile.repositories;

import com.application.profile.entities.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Modifying
    @Query("delete from PostBookmark pb where pb.post.id = :postId and pb.user.id = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    // Same @EntityGraph pattern PostRepository already uses to avoid N+1 when
    // serializing each bookmarked post's media list.
    @EntityGraph(attributePaths = {"post", "post.media"})
    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
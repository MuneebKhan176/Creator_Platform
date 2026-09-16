// src/main/java/com/application/comment/repositories/CommentRepository.java
package com.application.comment.repositories;

import com.application.comment.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    // Batched count for a page of posts, so the feed doesn't run one COUNT
    // query per post (N+1) just to show comment counts.
    @Query("select c.post.id as postId, count(c) as total from Comment c where c.post.id in :postIds group by c.post.id")
    List<PostCommentCount> countGroupedByPostIds(@Param("postIds") List<Long> postIds);

    interface PostCommentCount {
        Long getPostId();
        Long getTotal();
    }
}
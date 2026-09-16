// src/main/java/com/application/post/repositories/PostRepository.java
package com.application.post.repositories;

import com.application.post.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // @EntityGraph avoids an N+1 query when each post's media list is serialized.
    // Pageable input keeps pagination trivial to introduce on the frontend later.
    @EntityGraph(attributePaths = "media")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
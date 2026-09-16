// src/main/java/com/application/profile/repositories/UserFollowRepository.java
package com.application.profile.repositories;

import com.application.authentication.entities.User;
import com.application.profile.entities.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long countByFolloweeId(Long followeeId);

    long countByFollowerId(Long followerId);

    @Modifying
    @Query("delete from UserFollow uf where uf.follower.id = :followerId and uf.followee.id = :followeeId")
    void deleteByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    // Users who follow :userId — newest follower first.
    @Query("select uf.follower from UserFollow uf where uf.followee.id = :userId order by uf.createdAt desc")
    Page<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    // Users :userId follows — newest first.
    @Query("select uf.followee from UserFollow uf where uf.follower.id = :userId order by uf.createdAt desc")
    Page<User> findFollowing(@Param("userId") Long userId, Pageable pageable);
}
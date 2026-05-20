package com.example.canvasia.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.canvasia.entity.Follow;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    boolean existsByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    long countByFollowerUsername(String followerUsername);

    long countByFollowingUsername(String followingUsername);

    Page<Follow> findByFollowingUsernameOrderByCreatedAtDesc(String followingUsername, Pageable pageable);

    Page<Follow> findByFollowerUsernameOrderByCreatedAtDesc(String followerUsername, Pageable pageable);
}

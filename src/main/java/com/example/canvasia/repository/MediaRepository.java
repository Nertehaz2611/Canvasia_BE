package com.example.canvasia.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Media;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    @Query("SELECT m.userId FROM Media m WHERE m.id = :mediaId")
    Optional<UUID> findUserIdByMediaId(@Param("mediaId") UUID mediaId);

    @Query("SELECT m FROM Media m JOIN FETCH m.post p JOIN FETCH p.user u WHERE m.id = :mediaId")
    Optional<Media> findByIdWithPostAndUser(@Param("mediaId") UUID mediaId);

    List<Media> findByPostIdInOrderByOrderIndexAsc(List<UUID> postIds);

    List<Media> findByPostIdOrderByOrderIndexAsc(UUID postId);

    Page<Media> findByPostIdOrderByOrderIndexAsc(UUID postId, Pageable pageable);

    Page<Media> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);
}

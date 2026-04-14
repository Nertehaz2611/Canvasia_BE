package com.example.canvasia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.canvasia.entity.Media;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findByPostIdInOrderByOrderIndexAsc(List<UUID> postIds);

    List<Media> findByPostIdOrderByOrderIndexAsc(UUID postId);

    Page<Media> findByPostIdOrderByOrderIndexAsc(UUID postId, Pageable pageable);

    Page<Media> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);
}

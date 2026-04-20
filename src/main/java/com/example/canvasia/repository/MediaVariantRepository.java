package com.example.canvasia.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.enums.MediaVariantType;

public interface MediaVariantRepository extends JpaRepository<MediaVariant, UUID> {

    List<MediaVariant> findByMediaIdInAndType(List<UUID> mediaIds, MediaVariantType type);

    List<MediaVariant> findByMediaIdIn(List<UUID> mediaIds);

    void deleteByMediaIdIn(List<UUID> mediaIds);

    @EntityGraph(attributePaths = {"media", "media.post"})
    Page<MediaVariant> findByTypeAndMediaPostIsDeletedFalse(MediaVariantType type, Pageable pageable);

    @EntityGraph(attributePaths = {"media", "media.post"})
    @Query("""
        select mv from MediaVariant mv
        where mv.type = :type
          and mv.media.post.isDeleted = false
        order by mv.media.post.createdAt desc, mv.media.id desc
        """)
    List<MediaVariant> findThumbnailDiscoverFirstPage(
        @Param("type") MediaVariantType type,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"media", "media.post"})
    @Query("""
        select mv from MediaVariant mv
        where mv.type = :type
          and mv.media.post.isDeleted = false
          and (
            mv.media.post.createdAt < :cursorCreatedAt
            or (mv.media.post.createdAt = :cursorCreatedAt and mv.media.id < :cursorMediaId)
          )
        order by mv.media.post.createdAt desc, mv.media.id desc
        """)
    List<MediaVariant> findThumbnailDiscoverSlice(
        @Param("type") MediaVariantType type,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorMediaId") UUID cursorMediaId,
        Pageable pageable
    );
}

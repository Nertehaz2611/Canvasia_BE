package com.example.canvasia.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Post;
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByUserUsernameAndIsDeletedFalse(String username, Pageable pageable);

    Page<Post> findByUserUsernameAndIsDeletedTrue(String username, Pageable pageable);

    Optional<Post> findByIdAndUserUsername(UUID id, String username);

    Optional<Post> findByIdAndUserUsernameAndIsDeletedTrue(UUID id, String username);

    List<Post> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);

    @Query(
            value = """
                    select distinct p.*
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and (
                        lower(trim(t.name)) = lower(trim(:tagName))
                        or lower(trim(t.name)) = lower(trim(:legacyTagName))
                      )
                      and t.type = :tagType
                    order by p.created_at desc, p.id desc
                    """,
            countQuery = """
                    select count(distinct p.id)
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and (
                        lower(trim(t.name)) = lower(trim(:tagName))
                        or lower(trim(t.name)) = lower(trim(:legacyTagName))
                      )
                      and t.type = :tagType
                    """,
            nativeQuery = true
    )
    Page<Post> findByTag(
            @Param("tagName") String tagName,
            @Param("legacyTagName") String legacyTagName,
            @Param("tagType") String tagType,
            Pageable pageable
    );

    @Query("""
        select p from Post p
        where p.isDeleted = false
        order by p.createdAt desc, p.id desc
        """)
    List<Post> findDiscoverFirstPage(Pageable pageable);

    @Query("""
        select p from Post p
        where p.isDeleted = false
          and (
            p.createdAt < :cursorCreatedAt
            or (p.createdAt = :cursorCreatedAt and p.id < :cursorId)
          )
        order by p.createdAt desc, p.id desc
        """)
    List<Post> findDiscoverSlice(
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Query(
            value = """
                    select distinct p.*
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and (
                        lower(trim(t.name)) = lower(trim(:tagName))
                        or lower(trim(t.name)) = lower(trim(:legacyTagName))
                      )
                      and t.type = :tagType
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> findDiscoverFirstPageByTag(
        @Param("tagName") String tagName,
        @Param("legacyTagName") String legacyTagName,
        @Param("tagType") String tagType,
        Pageable pageable
    );

    @Query(
            value = """
                    select distinct p.*
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and (
                        lower(trim(t.name)) = lower(trim(:tagName))
                        or lower(trim(t.name)) = lower(trim(:legacyTagName))
                      )
                      and t.type = :tagType
                      and (
                        p.created_at < :cursorCreatedAt
                        or (p.created_at = :cursorCreatedAt and p.id < :cursorId)
                      )
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> findDiscoverSliceByTag(
        @Param("tagName") String tagName,
      @Param("legacyTagName") String legacyTagName,
        @Param("tagType") String tagType,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );
}

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

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndIsPendingFalse();

    @Query(
            value = """
                    select p.*
                    from posts p
                    join users u on u.id = p.user_id
                    where u.username = :username
                      and coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        :viewerUsername = :username
                        or coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                    order by p.created_at desc, p.id desc
                    """,
            countQuery = """
                    select count(*)
                    from posts p
                    join users u on u.id = p.user_id
                    where u.username = :username
                      and coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        :viewerUsername = :username
                        or coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                    """,
            nativeQuery = true
    )
    Page<Post> findVisiblePostsByUserUsername(
            @Param("username") String username,
            @Param("viewerUsername") String viewerUsername,
            Pageable pageable
    );

    Page<Post> findByUserUsernameAndIsDeletedFalseAndIsPendingTrue(String username, Pageable pageable);

    Page<Post> findByIsDeletedFalseAndIsPendingTrueAndIsRejectedFalse(Pageable pageable);

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
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
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
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
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
            @Param("viewerUsername") String viewerUsername,
            Pageable pageable
    );

    @Query(
            value = """
                    select p.*
                    from posts p
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> findDiscoverFirstPage(@Param("viewerUsername") String viewerUsername, Pageable pageable);

    @Query(
            value = """
                    select p.*
                    from posts p
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                      and (
                        p.created_at < :cursorCreatedAt
                        or (p.created_at = :cursorCreatedAt and p.id < :cursorId)
                      )
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> findDiscoverSlice(
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") UUID cursorId,
        @Param("viewerUsername") String viewerUsername,
        Pageable pageable
    );

    @Query(
            value = """
                    select distinct p.*
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
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
      @Param("viewerUsername") String viewerUsername,
        Pageable pageable
    );

    @Query(
            value = """
                    select distinct p.*
                    from posts p
                    join post_tag pt on pt.post_id = p.id
                    join tags t on t.id = pt.tag_id
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
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
      @Param("viewerUsername") String viewerUsername,
        Pageable pageable
    );

    @Query(
            value = """
                    select p.*
                    from posts p
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                      and (
                        lower(coalesce(p.caption, '')) like lower(concat('%', :query, '%'))
                        or to_tsvector('simple', coalesce(p.caption, '')) @@ websearch_to_tsquery('simple', :query)
                      )
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> searchDiscoverFirstPage(
            @Param("query") String query,
          @Param("viewerUsername") String viewerUsername,
            Pageable pageable
    );

    @Query(
            value = """
                    select p.*
                    from posts p
                    where coalesce(p.is_deleted, false) = false
                      and coalesce(p.is_pending, false) = false
                      and (
                        coalesce(p.visibility, 'PUBLIC') = 'PUBLIC'
                        or (
                          :viewerUsername is not null
                          and exists (select 1 from users owner where owner.id = p.user_id and owner.username = :viewerUsername)
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'FOLLOWERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from follows f
                            join users fu on fu.id = f.follower_id
                            where f.following_id = p.user_id
                              and fu.username = :viewerUsername
                          )
                        )
                        or (
                          coalesce(p.visibility, 'PUBLIC') = 'SELECTED_USERS'
                          and :viewerUsername is not null
                          and exists (
                            select 1
                            from post_allowed_viewers pav
                            join users au on au.id = pav.user_id
                            where pav.post_id = p.id
                              and au.username = :viewerUsername
                          )
                        )
                      )
                      and (
                        lower(coalesce(p.caption, '')) like lower(concat('%', :query, '%'))
                        or to_tsvector('simple', coalesce(p.caption, '')) @@ websearch_to_tsquery('simple', :query)
                      )
                      and (
                        p.created_at < :cursorCreatedAt
                        or (p.created_at = :cursorCreatedAt and p.id < :cursorId)
                      )
                    order by p.created_at desc, p.id desc
                    """,
            nativeQuery = true
    )
    List<Post> searchDiscoverSlice(
            @Param("query") String query,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
          @Param("viewerUsername") String viewerUsername,
            Pageable pageable
    );

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
    Optional<Post> findByIdWithUserEager(@Param("postId") UUID postId);

    @Query(value = "SELECT user_id FROM posts WHERE id = :postId", nativeQuery = true)
    Optional<UUID> findUserIdByPostId(@Param("postId") UUID postId);
}

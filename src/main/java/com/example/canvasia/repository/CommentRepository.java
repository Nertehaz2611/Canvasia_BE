package com.example.canvasia.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    interface CommentPostRefView {
        UUID getCommentId();

        UUID getPostId();
    }

    interface PostCommentCountView {
        UUID getPostId();

        long getCommentCount();
    }

    interface ReplyCountView {
        UUID getParentId();

        long getReplyCount();
    }

    @Query("""
        select c from Comment c
        where c.post.id = :postId
          and c.parent is null
        """)
    Page<Comment> findRootCommentsByPostId(@Param("postId") UUID postId, Pageable pageable);

    @Query("""
        select c from Comment c
        where c.post.isDeleted = false
          and c.post.isPending = false
        order by c.createdAt desc
        """)
    List<Comment> findLatestComments(Pageable pageable);

    Optional<Comment> findByIdAndPostId(UUID id, UUID postId);

    @Query("""
        select count(c) from Comment c
        where c.post.isDeleted = false
          and c.post.isPending = false
        """)
    long countActiveComments();

    @Query("""
        select distinct c.user.id
        from Comment c
        where c.id = :rootId
           or c.rootId = :rootId
        """)
    List<UUID> findParticipantUserIdsByRootId(@Param("rootId") UUID rootId);

    @Query("""
        select c.id
        from Comment c
        where c.post.id = :postId
        """)
    List<UUID> findIdsByPostId(@Param("postId") UUID postId);

    @Query("""
        select c from Comment c
        where c.post.id = :postId
          and c.rootId in :rootIds
        order by c.createdAt asc, c.id asc
        """)
    List<Comment> findThreadCommentsByPostIdAndRootIds(
            @Param("postId") UUID postId,
            @Param("rootIds") Collection<UUID> rootIds
    );

    @Query("""
        select c.parent.id as parentId, count(c.id) as replyCount
        from Comment c
        where c.parent.id in :parentIds
        group by c.parent.id
        """)
    List<ReplyCountView> countRepliesByParentIds(@Param("parentIds") Collection<UUID> parentIds);

    @Query("""
        select c.id
        from Comment c
        where c.rootId = :rootId
        """)
    List<UUID> findIdsByRootId(@Param("rootId") UUID rootId);

    @Query("""
        select c.id as commentId, c.post.id as postId
        from Comment c
        where c.id in :commentIds
        """)
    List<CommentPostRefView> findPostIdsByCommentIds(@Param("commentIds") Collection<UUID> commentIds);

    @Modifying
    @Query("""
        delete from Comment c
        where c.rootId = :rootId
        """)
    int deleteByRootId(@Param("rootId") UUID rootId);

    @Modifying
    @Query("""
        delete from Comment c
        where c.post.id = :postId
        """)
    int deleteByPostId(@Param("postId") UUID postId);

    @Query("""
        select c.post.id as postId, count(c.id) as commentCount
        from Comment c
        where c.post.id in :postIds
        group by c.post.id
        """)
    List<PostCommentCountView> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    long countByPostId(UUID postId);
}

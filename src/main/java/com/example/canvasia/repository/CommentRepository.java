package com.example.canvasia.repository;

import com.example.canvasia.entity.Comment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

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

    Optional<Comment> findByIdAndPostId(UUID id, UUID postId);

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

    @Modifying
    @Query("""
        delete from Comment c
        where c.rootId = :rootId
        """)
    int deleteByRootId(@Param("rootId") UUID rootId);
}

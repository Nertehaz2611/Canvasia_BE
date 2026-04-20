package com.example.canvasia.repository;

import com.example.canvasia.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    interface CommentLikeCountView {
        UUID getCommentId();

        long getLikeCount();
    }

    boolean existsByUserUsernameAndCommentId(String username, UUID commentId);

    long countByCommentId(UUID commentId);

    Optional<CommentLike> findByUserUsernameAndCommentId(String username, UUID commentId);

    void deleteByCommentId(UUID commentId);

    void deleteByCommentIdIn(Collection<UUID> commentIds);

    @Query("""
        select cl.comment.id as commentId, count(cl.id) as likeCount
        from CommentLike cl
        where cl.comment.id in :commentIds
        group by cl.comment.id
        """)
    List<CommentLikeCountView> countByCommentIds(@Param("commentIds") Collection<UUID> commentIds);

    @Query("""
        select cl.comment.id
        from CommentLike cl
        where cl.user.username = :username
          and cl.comment.id in :commentIds
        """)
    List<UUID> findLikedCommentIdsByUsernameAndCommentIds(
            @Param("username") String username,
            @Param("commentIds") Collection<UUID> commentIds
    );
}

package com.example.canvasia.repository;

import com.example.canvasia.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    interface PostLikeCountView {
        UUID getPostId();

        long getLikeCount();
    }

    boolean existsByUserUsernameAndPostId(String username, UUID postId);

    long countByPostId(UUID postId);

    Optional<PostLike> findByUserUsernameAndPostId(String username, UUID postId);

    @Query("""
        select pl.post.id as postId, count(pl.id) as likeCount
        from PostLike pl
        where pl.post.id in :postIds
        group by pl.post.id
        """)
    List<PostLikeCountView> countByPostIds(@Param("postIds") List<UUID> postIds);

    @Query("""
        select pl.post.id
        from PostLike pl
        where pl.user.username = :username
          and pl.post.id in :postIds
        """)
    List<UUID> findLikedPostIdsByUsernameAndPostIds(
            @Param("username") String username,
            @Param("postIds") List<UUID> postIds
    );
}

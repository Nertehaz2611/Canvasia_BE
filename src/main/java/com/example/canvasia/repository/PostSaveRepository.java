package com.example.canvasia.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.PostSave;

public interface PostSaveRepository extends JpaRepository<PostSave, UUID> {

    boolean existsByUserUsernameAndPostId(String username, UUID postId);

    Optional<PostSave> findByUserUsernameAndPostId(String username, UUID postId);

    void deleteByPostId(UUID postId);

    @Query("""
        select ps.post.id
        from PostSave ps
        where ps.user.username = :username
          and ps.post.id in :postIds
        """)
    List<UUID> findSavedPostIdsByUsernameAndPostIds(
            @Param("username") String username,
            @Param("postIds") List<UUID> postIds
    );

    @Query("""
        select ps.post from PostSave ps
        where ps.user.username = :username
          and ps.post.isDeleted = false
        order by ps.createdAt desc, ps.post.id desc
        """)
    Page<com.example.canvasia.entity.Post> findSavedPostsByUsername(
            @Param("username") String username,
            Pageable pageable
    );
}

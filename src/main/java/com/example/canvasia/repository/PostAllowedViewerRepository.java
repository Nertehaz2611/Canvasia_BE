package com.example.canvasia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.PostAllowedViewer;

public interface PostAllowedViewerRepository extends JpaRepository<PostAllowedViewer, UUID> {

    @Query("""
        select pav from PostAllowedViewer pav
        join fetch pav.user u
        where pav.post.id = :postId
        """)
    List<PostAllowedViewer> findByPostIdWithUser(@Param("postId") UUID postId);

    @Query("""
        select pav from PostAllowedViewer pav
        join fetch pav.user u
        where pav.post.id in :postIds
        """)
    List<PostAllowedViewer> findByPostIdInWithUser(@Param("postIds") List<UUID> postIds);

    boolean existsByPostIdAndUserUsername(UUID postId, String username);

    void deleteByPostId(UUID postId);
}

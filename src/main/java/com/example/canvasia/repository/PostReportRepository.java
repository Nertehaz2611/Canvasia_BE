package com.example.canvasia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.PostReport;

public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

    boolean existsByReporterUsernameAndPostId(String username, UUID postId);

    long countByPostId(UUID postId);

    @Query("""
        select distinct pr.post.id
        from PostReport pr
        where pr.post.isDeleted = false
        order by pr.post.id
        """)
    Page<UUID> findDistinctReportedPostIds(Pageable pageable);

    @Query("""
        select pr from PostReport pr
        join fetch pr.reporter
        where pr.post.id in :postIds
        """)
    List<PostReport> findByPostIdIn(@Param("postIds") List<UUID> postIds);

    void deleteByPostId(UUID postId);
}

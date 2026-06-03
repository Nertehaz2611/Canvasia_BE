package com.example.canvasia.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.canvasia.entity.PostReport;

public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

    boolean existsByReporterUsernameAndPostId(String username, UUID postId);
}

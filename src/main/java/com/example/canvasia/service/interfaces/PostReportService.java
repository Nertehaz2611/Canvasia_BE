package com.example.canvasia.service.interfaces;

import java.util.UUID;

import com.example.canvasia.dto.post.CreatePostReportRequest;

public interface PostReportService {

    void reportPost(String reporterUsername, UUID postId, CreatePostReportRequest request);
}

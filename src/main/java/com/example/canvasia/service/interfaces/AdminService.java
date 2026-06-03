package com.example.canvasia.service.interfaces;

import java.util.UUID;

import com.example.canvasia.dto.admin.AdminPendingPostFeedResponse;
import com.example.canvasia.dto.admin.AdminReportedPostFeedResponse;
import com.example.canvasia.dto.admin.AdminStatsResponse;
import com.example.canvasia.dto.admin.AdminUserFeedResponse;

public interface AdminService {

    AdminStatsResponse getStats();

    AdminUserFeedResponse getUsers(int page, int size);

    AdminPendingPostFeedResponse getPendingPosts(int page, int size);

    void approvePendingPost(UUID postId, String adminUsername);

    void rejectPendingPost(UUID postId, String adminUsername);

    AdminReportedPostFeedResponse getReportedPosts(int page, int size);

    void deleteReportedPost(UUID postId, String adminUsername);
}

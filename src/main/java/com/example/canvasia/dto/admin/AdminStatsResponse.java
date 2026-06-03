package com.example.canvasia.dto.admin;

public record AdminStatsResponse(
        long totalUsers,
        long newUsersLast7Days,
        long totalPosts,
        long totalComments,
        long totalLikes,
        long totalReports
) {
}

package com.example.canvasia.dto.follow;

public record FollowStatusResponse(
        String username,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {
}

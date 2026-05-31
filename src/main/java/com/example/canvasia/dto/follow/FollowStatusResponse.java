package com.example.canvasia.dto.follow;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FollowStatusResponse(
        String username,
        long followerCount,
        long followingCount,
        @JsonProperty("isFollowing") boolean isFollowing
) {
}

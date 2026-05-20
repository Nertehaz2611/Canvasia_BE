package com.example.canvasia.dto.follow;

import java.util.UUID;

public record FollowUserItemResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl
) {
}

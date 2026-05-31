package com.example.canvasia.dto.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID postId,
        UUID userId,
        String displayName,
        String username,
        String avatarUrl,
        String caption,
        LocalDateTime createdAt,
        List<MediaItemResponse> media,
        List<String> tags,
        long commentCount,
        long likeCount,
        boolean likedByMe,
        boolean isPending,
        UUID flaggedMatchedPostId,
        String flaggedMatchedAuthorDisplayName
) {
}

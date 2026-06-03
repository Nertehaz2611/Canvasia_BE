package com.example.canvasia.dto.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.canvasia.dto.post.MediaItemResponse;

public record AdminPendingPostItem(
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
        UUID flaggedMatchedPostId,
        String flaggedMatchedAuthorDisplayName
) {
}

package com.example.canvasia.dto.discover;

import java.time.LocalDateTime;
import java.util.UUID;

public record LatestDiscussionResponse(
        UUID commentId,
        UUID postId,
        UUID userId,
        String displayName,
        String username,
        String content,
        LocalDateTime createdAt
) {
}

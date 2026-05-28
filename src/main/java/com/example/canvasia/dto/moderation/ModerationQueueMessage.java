package com.example.canvasia.dto.moderation;

public record ModerationQueueMessage(
        String action,
        String mediaId,
        String imageUrl
) {
}

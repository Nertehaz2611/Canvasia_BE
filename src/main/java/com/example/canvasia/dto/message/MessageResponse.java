package com.example.canvasia.dto.message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String senderUsername,
        String senderDisplayName,
        String senderAvatarUrl,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

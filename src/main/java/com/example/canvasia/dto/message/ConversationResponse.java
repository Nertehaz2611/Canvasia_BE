package com.example.canvasia.dto.message;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID otherUserId,
        String otherUsername,
        String otherDisplayName,
        String otherAvatarUrl,
        String lastMessagePreview,
        UUID lastSenderId,
        LocalDateTime lastMessageAt,
        int unreadCount
) {
}

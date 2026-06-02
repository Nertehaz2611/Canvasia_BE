package com.example.canvasia.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        String type,
        String referenceType,
        UUID referenceId,
        UUID postId,
        String content,
        boolean isRead,
        UUID actorId,
        String actorUsername,
        String actorDisplayName,
        String actorAvatarUrl,
        LocalDateTime createdAt
) {
}
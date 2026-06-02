package com.example.canvasia.dto.notification;

import java.util.List;

public record NotificationFeedResponse(
        List<NotificationResponse> notifications,
        long unreadCount,
        int page,
        int size,
        boolean hasNext
) {
}
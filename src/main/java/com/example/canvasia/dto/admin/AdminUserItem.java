package com.example.canvasia.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserItem(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        String role,
        String status,
        LocalDateTime createdAt
) {
}

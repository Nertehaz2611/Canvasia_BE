package com.example.canvasia.dto.moderation;

import java.util.UUID;

public record ModerationCallbackRequest(
        UUID mediaId,
        UUID matchedMediaId,
        Double similarity,
        String status
) {
}

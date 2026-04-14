package com.example.canvasia.dto.post;

import java.util.UUID;

public record ThumbnailItemResponse(
        UUID mediaId,
        UUID postId,
        UUID userId,
        Integer orderIndex,
        String thumbnailPublicId,
        String thumbnailUrl
) {
}

package com.example.canvasia.dto.media;

import java.util.UUID;

public record MediaQueryItemResponse(
        UUID mediaId,
        UUID postId,
        UUID userId,
        Integer orderIndex,
        String originalPublicId,
        String originalUrl,
        String thumbnailPublicId,
        String thumbnailUrl
) {
}

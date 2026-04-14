package com.example.canvasia.dto.post;

import java.util.UUID;

public record MediaItemResponse(
        UUID mediaId,
        Integer orderIndex,
        String originalPublicId,
        String originalUrl
) {
}

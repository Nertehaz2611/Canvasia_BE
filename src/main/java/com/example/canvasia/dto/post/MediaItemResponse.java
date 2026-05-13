package com.example.canvasia.dto.post;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record MediaItemResponse(
        @Schema(description = "Media item identifier")
        UUID mediaId,
        @Schema(description = "Order index within the post")
        Integer orderIndex,
        @Schema(description = "Cloud storage public id for the original asset")
        String originalPublicId,
        @Schema(description = "Original asset URL")
        String originalUrl,
        @Schema(description = "Cloud storage public id for the thumbnail variant")
        String thumbnailPublicId,
        @Schema(description = "Thumbnail URL for grid and preview usage")
        String thumbnailUrl
) {
}

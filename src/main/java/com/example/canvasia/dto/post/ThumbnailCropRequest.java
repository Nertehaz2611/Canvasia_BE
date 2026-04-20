package com.example.canvasia.dto.post;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ThumbnailCropRequest(
        @NotNull(message = "thumbnailCrops[].index is required")
        @Min(value = 0, message = "thumbnailCrops[].index must be >= 0")
        Integer index,
        @NotNull(message = "thumbnailCrops[].x is required")
        @Min(value = 0, message = "thumbnailCrops[].x must be >= 0")
        Integer x,
        @NotNull(message = "thumbnailCrops[].y is required")
        @Min(value = 0, message = "thumbnailCrops[].y must be >= 0")
        Integer y,
        @NotNull(message = "thumbnailCrops[].width is required")
        @Positive(message = "thumbnailCrops[].width must be > 0")
        Integer width,
        @NotNull(message = "thumbnailCrops[].height is required")
        @Positive(message = "thumbnailCrops[].height must be > 0")
        Integer height
) {
}

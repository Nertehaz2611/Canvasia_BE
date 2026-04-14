package com.example.canvasia.dto.post;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReplaceMediaRequest(
        @NotNull(message = "replaceMedia[].mediaId is required")
        UUID mediaId,
        @NotNull(message = "replaceMedia[].fileIndex is required")
        @Min(value = 0, message = "replaceMedia[].fileIndex must be >= 0")
        Integer fileIndex
) {
}

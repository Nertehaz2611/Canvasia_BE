package com.example.canvasia.dto.post;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(max = 2200, message = "Caption must be <= 2200 characters")
        String caption,
        List<String> tags,
        List<UUID> deleteMediaIds,
        List<@Valid ReplaceMediaRequest> replaceMedia,
        List<@Valid ThumbnailCropRequest> thumbnailCrops
) {
}

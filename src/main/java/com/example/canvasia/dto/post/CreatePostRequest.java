package com.example.canvasia.dto.post;

import java.util.List;
import java.util.UUID;

import com.example.canvasia.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @Size(max = 2200, message = "Caption must be <= 2200 characters")
        String caption,
        List<String> tags,
        List<@Valid ThumbnailCropRequest> thumbnailCrops,
        PostVisibility visibility,
        List<UUID> allowedViewerUserIds
) {
}

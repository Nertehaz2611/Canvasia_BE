package com.example.canvasia.dto.post;

import java.util.UUID;

public record PostAllowedViewerResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl
) {
}

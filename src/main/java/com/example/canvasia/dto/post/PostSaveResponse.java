package com.example.canvasia.dto.post;

import java.util.UUID;

public record PostSaveResponse(
        UUID postId,
        boolean savedByMe
) {
}

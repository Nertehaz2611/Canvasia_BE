package com.example.canvasia.dto.post;

import java.util.UUID;

public record PostLikeResponse(
        UUID postId,
        long likeCount,
        boolean likedByMe
) {
}

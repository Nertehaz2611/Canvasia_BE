package com.example.canvasia.dto.comment;

import java.util.UUID;

public record CommentLikeResponse(
        UUID commentId,
        long likeCount,
        boolean likedByMe
) {
}

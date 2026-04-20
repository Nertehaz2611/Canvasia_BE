package com.example.canvasia.dto.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID commentId,
        UUID postId,
        UUID parentId,
        UUID rootId,
        UUID userId,
        String displayName,
        String username,
        String content,
        LocalDateTime createdAt,
        long likeCount,
        boolean likedByMe,
        long replyCount,
        List<CommentResponse> replies
) {
}

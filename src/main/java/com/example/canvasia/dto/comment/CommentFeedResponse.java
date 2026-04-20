package com.example.canvasia.dto.comment;

import java.util.List;

public record CommentFeedResponse(
        List<CommentResponse> items,
        int page,
        int size,
        boolean hasNext,
        int maxDepth
) {
}

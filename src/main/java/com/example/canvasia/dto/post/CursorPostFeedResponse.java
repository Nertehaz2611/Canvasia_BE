package com.example.canvasia.dto.post;

import java.util.List;

public record CursorPostFeedResponse(
        List<PostResponse> items,
        int limit,
        String nextCursor,
        boolean hasNext
) {
}

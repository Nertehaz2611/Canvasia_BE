package com.example.canvasia.dto.post;

import java.util.List;

public record CursorThumbnailFeedResponse(
        List<ThumbnailItemResponse> items,
        int limit,
        String nextCursor,
        boolean hasNext
) {
}

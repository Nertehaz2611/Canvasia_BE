package com.example.canvasia.dto.post;

import java.util.List;

public record PostFeedResponse(
        List<PostResponse> items,
        int page,
        int size,
        boolean hasNext
) {
}

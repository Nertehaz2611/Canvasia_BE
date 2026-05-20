package com.example.canvasia.dto.follow;

import java.util.List;

public record FollowUserFeedResponse(
        List<FollowUserItemResponse> items,
        int page,
        int size,
        boolean hasNext
) {
}

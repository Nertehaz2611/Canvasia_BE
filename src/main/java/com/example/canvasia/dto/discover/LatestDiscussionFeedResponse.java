package com.example.canvasia.dto.discover;

import java.util.List;

public record LatestDiscussionFeedResponse(
        List<LatestDiscussionResponse> items
) {
}

package com.example.canvasia.dto.media;

import java.util.List;

public record MediaListResponse(
        List<MediaQueryItemResponse> items,
        int page,
        int size,
        boolean hasNext
) {
}

package com.example.canvasia.dto.admin;

import java.util.List;

public record AdminPendingPostFeedResponse(
        List<AdminPendingPostItem> items,
        int page,
        int size,
        boolean hasNext
) {
}

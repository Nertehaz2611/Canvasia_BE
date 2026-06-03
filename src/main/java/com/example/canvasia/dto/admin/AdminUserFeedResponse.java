package com.example.canvasia.dto.admin;

import java.util.List;

public record AdminUserFeedResponse(
        List<AdminUserItem> items,
        int page,
        int size,
        boolean hasNext
) {
}

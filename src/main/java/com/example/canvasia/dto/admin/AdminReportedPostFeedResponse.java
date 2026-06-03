package com.example.canvasia.dto.admin;

import java.util.List;

public record AdminReportedPostFeedResponse(
        List<AdminReportedPostItem> items,
        int page,
        int size,
        boolean hasNext
) {
}

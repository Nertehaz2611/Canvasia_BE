package com.example.canvasia.dto.post;

import com.example.canvasia.enums.ReportReason;

import java.util.List;

public record CreatePostReportRequest(
        List<ReportReason> reasons,
        String otherReason
) {
}

package com.example.canvasia.dto.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminReportItem(
        UUID reportId,
        UUID reporterId,
        String reporterUsername,
        String reporterDisplayName,
        String reporterAvatarUrl,
        List<String> reasons,
        String otherReason,
        LocalDateTime reportedAt
) {
}

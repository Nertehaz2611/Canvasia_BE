package com.example.canvasia.dto.message;

import java.util.List;
import java.time.LocalDateTime;

public record MessagePageResponse(
        List<MessageResponse> messages,
        LocalDateTime nextCursor,
        boolean hasMore
) {
}

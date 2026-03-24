package com.example.canvasia.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
}

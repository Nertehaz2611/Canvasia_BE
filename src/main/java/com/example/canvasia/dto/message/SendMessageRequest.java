package com.example.canvasia.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        String recipientUsername,

        @NotBlank
        @Size(max = 2000)
        String content
) {
}

package com.example.canvasia.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content must not be blank")
        @Size(max = 2200, message = "Comment content must be <= 2200 characters")
        String content
) {
}

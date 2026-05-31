package com.example.canvasia.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortfolioRequest(
        @NotBlank(message = "Portfolio name must not be blank")
        @Size(max = 100, message = "Portfolio name must not exceed 100 characters")
        String name
) {
}

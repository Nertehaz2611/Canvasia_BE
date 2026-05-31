package com.example.canvasia.dto.portfolio;

import java.util.UUID;

public record PortfolioResponse(
        UUID portfolioId,
        String name,
        int mediaCount
) {
}

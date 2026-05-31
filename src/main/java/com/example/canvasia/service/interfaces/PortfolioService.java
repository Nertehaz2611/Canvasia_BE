package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.portfolio.PortfolioRequest;
import com.example.canvasia.dto.portfolio.PortfolioResponse;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    List<PortfolioResponse> getMyPortfolios(String username);

    List<PortfolioResponse> getPortfoliosByUsername(String username);

    PortfolioResponse createPortfolio(String username, PortfolioRequest request);

    void deletePortfolio(String username, UUID portfolioId);

    void addMediaToPortfolio(String username, UUID portfolioId, UUID mediaId);

    void removeMediaFromPortfolio(String username, UUID portfolioId, UUID mediaId);
}

package com.example.canvasia.controller;

import com.example.canvasia.dto.portfolio.PortfolioRequest;
import com.example.canvasia.dto.portfolio.PortfolioResponse;
import com.example.canvasia.service.interfaces.PortfolioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/my")
    public List<PortfolioResponse> getMyPortfolios(Authentication authentication) {
        return portfolioService.getMyPortfolios(authentication.getName());
    }

    @GetMapping("/users/{username}")
    public List<PortfolioResponse> getPortfoliosByUsername(@PathVariable String username) {
        return portfolioService.getPortfoliosByUsername(username);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse createPortfolio(
            Authentication authentication,
            @Valid @RequestBody PortfolioRequest request
    ) {
        return portfolioService.createPortfolio(authentication.getName(), request);
    }

    @DeleteMapping("/{portfolioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePortfolio(
            Authentication authentication,
            @PathVariable UUID portfolioId
    ) {
        portfolioService.deletePortfolio(authentication.getName(), portfolioId);
    }

    @PostMapping("/{portfolioId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMediaToPortfolio(
            Authentication authentication,
            @PathVariable UUID portfolioId,
            @PathVariable UUID mediaId
    ) {
        portfolioService.addMediaToPortfolio(authentication.getName(), portfolioId, mediaId);
    }

    @DeleteMapping("/{portfolioId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMediaFromPortfolio(
            Authentication authentication,
            @PathVariable UUID portfolioId,
            @PathVariable UUID mediaId
    ) {
        portfolioService.removeMediaFromPortfolio(authentication.getName(), portfolioId, mediaId);
    }
}

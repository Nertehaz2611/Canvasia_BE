package com.example.canvasia.service.impl;

import com.example.canvasia.dto.portfolio.PortfolioRequest;
import com.example.canvasia.dto.portfolio.PortfolioResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Portfolio;
import com.example.canvasia.entity.PortfolioMedia;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.PortfolioMediaRepository;
import com.example.canvasia.repository.PortfolioRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioMediaRepository portfolioMediaRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getMyPortfolios(String username) {
        return loadPortfoliosForUser(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfoliosByUsername(String username) {
        return loadPortfoliosForUser(username);
    }

    private List<PortfolioResponse> loadPortfoliosForUser(String username) {
        User user = requireUser(username);
        return toResponseList(portfolioRepository.findByUserIdOrderByCreatedAtAsc(user.getId()));
    }

    @Override
    @Transactional
    public PortfolioResponse createPortfolio(String username, PortfolioRequest request) {
        User user = requireUser(username);
        Portfolio portfolio = Portfolio.create(user, request.name().trim());
        portfolio = portfolioRepository.save(portfolio);
        return toResponse(portfolio, 0);
    }

    @Override
    @Transactional
    public void deletePortfolio(String username, UUID portfolioId) {
        Portfolio portfolio = requireOwnPortfolio(username, portfolioId);
        portfolioMediaRepository.deleteByPortfolioId(portfolio.getId());
        portfolioRepository.delete(portfolio);
    }

    @Override
    @Transactional
    public void addMediaToPortfolio(String username, UUID portfolioId, UUID mediaId) {
        Portfolio portfolio = requireOwnPortfolio(username, portfolioId);
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        if (!media.getUserId().equals(portfolio.getUser().getId())) {
            throw new IllegalArgumentException("You can only add your own media to a portfolio");
        }

        if (portfolioMediaRepository.existsByPortfolioIdAndMediaId(portfolioId, mediaId)) {
            return; // idempotent
        }

        portfolioMediaRepository.save(PortfolioMedia.create(portfolio, media));
    }

    @Override
    @Transactional
    public void removeMediaFromPortfolio(String username, UUID portfolioId, UUID mediaId) {
        requireOwnPortfolio(username, portfolioId);
        portfolioMediaRepository.findByPortfolioIdAndMediaId(portfolioId, mediaId)
                .ifPresent(portfolioMediaRepository::delete);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Portfolio requireOwnPortfolio(String username, UUID portfolioId) {
        User user = requireUser(username);
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
        if (!portfolio.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Portfolio does not belong to this user");
        }
        return portfolio;
    }

    private List<PortfolioResponse> toResponseList(List<Portfolio> portfolios) {
        return portfolios.stream()
                .map(p -> toResponse(p, (int) portfolioMediaRepository.countByPortfolioId(p.getId())))
                .toList();
    }

    private PortfolioResponse toResponse(Portfolio portfolio, int mediaCount) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName(), mediaCount);
    }
}

package com.example.canvasia.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.media.MediaListResponse;
import com.example.canvasia.dto.media.MediaQueryItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Portfolio;
import com.example.canvasia.entity.PortfolioMedia;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.PortfolioMediaRepository;
import com.example.canvasia.repository.PortfolioRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.impl.media.MediaQueryAssembler;
import com.example.canvasia.service.interfaces.MediaService;

import lombok.RequiredArgsConstructor;

import static com.example.canvasia.service.impl.support.PagingUtils.clampPageSize;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final int MAX_MEDIA_PAGE_SIZE = 50;

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioMediaRepository portfolioMediaRepository;
    private final MediaQueryAssembler mediaQueryAssembler;

    @Override
    @Transactional(readOnly = true)
    public MediaListResponse getMediaByPost(UUID postId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_MEDIA_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("orderIndex"), Sort.Order.desc("id")));

        Page<Media> mediaPage = mediaRepository.findByPostIdOrderByOrderIndexAsc(postId, pageable);
        List<MediaQueryItemResponse> items = mediaQueryAssembler.toMediaItems(mediaPage.getContent());

        return new MediaListResponse(items, safePage, safeSize, mediaPage.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaListResponse getMediaByUser(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_MEDIA_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("id")));

        Page<Media> mediaPage = mediaRepository.findByUserIdOrderByIdDesc(user.getId(), pageable);
        List<MediaQueryItemResponse> items = mediaQueryAssembler.toMediaItems(mediaPage.getContent());

        return new MediaListResponse(items, safePage, safeSize, mediaPage.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaListResponse getMediaByPortfolio(UUID portfolioId, int page, int size) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_MEDIA_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("id")));

        Page<PortfolioMedia> portfolioMediaPage = portfolioMediaRepository.findByPortfolioId(portfolio.getId(), pageable);
        List<Media> mediaList = portfolioMediaPage.getContent().stream().map(PortfolioMedia::getMedia).toList();
        List<MediaQueryItemResponse> items = mediaQueryAssembler.toMediaItems(mediaList);

        return new MediaListResponse(items, safePage, safeSize, portfolioMediaPage.hasNext());
    }
}

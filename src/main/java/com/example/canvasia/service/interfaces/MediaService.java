package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.media.MediaListResponse;

import java.util.UUID;

public interface MediaService {

    MediaListResponse getMediaByPost(UUID postId, int page, int size);

    MediaListResponse getMediaByUser(String username, int page, int size);

    MediaListResponse getMediaByPortfolio(UUID portfolioId, int page, int size);
}

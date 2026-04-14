package com.example.canvasia.controller;

import com.example.canvasia.dto.media.MediaListResponse;
import com.example.canvasia.service.interfaces.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/posts/{postId}")
    public MediaListResponse getMediaByPost(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return mediaService.getMediaByPost(postId, page, size);
    }

    @GetMapping("/users/{username}")
    public MediaListResponse getMediaByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return mediaService.getMediaByUser(username, page, size);
    }

    @GetMapping("/portfolios/{portfolioId}")
    public MediaListResponse getMediaByPortfolio(
            @PathVariable UUID portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return mediaService.getMediaByPortfolio(portfolioId, page, size);
    }
}

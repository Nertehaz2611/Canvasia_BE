package com.example.canvasia.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.discover.LatestDiscussionFeedResponse;
import com.example.canvasia.dto.discover.LatestHashtagResponse;
import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.CursorThumbnailFeedResponse;
import com.example.canvasia.service.interfaces.DiscoverService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final DiscoverService discoverService;

    @GetMapping("/posts")
    public CursorPostFeedResponse getPostFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String tag
    ) {
        return discoverService.getPostFeed(limit, cursor, tag, extractViewerUsername(authentication));
    }

    @GetMapping("/thumbnails")
    public CursorThumbnailFeedResponse getThumbnails(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor
    ) {
        return discoverService.getThumbnailFeed(limit, cursor);
    }

    @GetMapping("/latest-discussions")
    public LatestDiscussionFeedResponse getLatestDiscussions(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return discoverService.getLatestDiscussions(limit);
    }

    @GetMapping("/latest-hashtags")
    public LatestHashtagResponse getLatestHashtags(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return discoverService.getLatestHashtags(limit);
    }

    private String extractViewerUsername(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}

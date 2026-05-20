package com.example.canvasia.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.follow.FollowStatusResponse;
import com.example.canvasia.dto.follow.FollowUserFeedResponse;
import com.example.canvasia.service.interfaces.FollowService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{username}")
    public FollowStatusResponse follow(
            Authentication authentication,
            @PathVariable String username
    ) {
        return followService.follow(authentication.getName(), username);
    }

    @DeleteMapping("/{username}")
    public FollowStatusResponse unfollow(
            Authentication authentication,
            @PathVariable String username
    ) {
        return followService.unfollow(authentication.getName(), username);
    }

    @GetMapping("/{username}/followers")
    public FollowUserFeedResponse getFollowers(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followService.getFollowers(username, page, size);
    }

    @GetMapping("/{username}/following")
    public FollowUserFeedResponse getFollowing(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followService.getFollowing(username, page, size);
    }
}

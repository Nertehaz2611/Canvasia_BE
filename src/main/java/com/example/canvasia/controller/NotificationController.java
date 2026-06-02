package com.example.canvasia.controller;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.notification.NotificationFeedResponse;
import com.example.canvasia.service.interfaces.NotificationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public NotificationFeedResponse getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(extractUsername(authentication), page, size);
    }

    @PostMapping("/{notificationId}/read")
    @SecurityRequirement(name = "bearerAuth")
    public void markAsRead(Authentication authentication, @PathVariable UUID notificationId) {
        notificationService.markAsRead(extractUsername(authentication), notificationId);
    }

    @PostMapping("/read-all")
    @SecurityRequirement(name = "bearerAuth")
    public void markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(extractUsername(authentication));
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalArgumentException("Authentication required");
        }
        return authentication.getName();
    }
}
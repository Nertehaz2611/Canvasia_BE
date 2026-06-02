package com.example.canvasia.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.moderation.ModerationCallbackRequest;
import com.example.canvasia.service.interfaces.ModerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody ModerationCallbackRequest request) {
        moderationService.handleCallback(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/posts/{postId}/approve")
    public ResponseEntity<Void> approvePendingPost(Authentication authentication, @PathVariable UUID postId) {
        requireAdmin(authentication);
        moderationService.approvePendingPost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/posts/{postId}/delete")
    public ResponseEntity<Void> deletePendingPost(Authentication authentication, @PathVariable UUID postId) {
        requireAdmin(authentication);
        moderationService.deletePendingPost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}

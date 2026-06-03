package com.example.canvasia.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.canvasia.dto.admin.AdminPendingPostFeedResponse;
import com.example.canvasia.dto.admin.AdminReportedPostFeedResponse;
import com.example.canvasia.dto.admin.AdminStatsResponse;
import com.example.canvasia.dto.admin.AdminUserFeedResponse;
import com.example.canvasia.service.interfaces.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse getStats(Authentication authentication) {
        requireAdmin(authentication);
        return adminService.getStats();
    }

    @GetMapping("/users")
    public AdminUserFeedResponse getUsers(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(authentication);
        return adminService.getUsers(page, size);
    }

    @GetMapping("/posts/pending")
    public AdminPendingPostFeedResponse getPendingPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        requireAdmin(authentication);
        return adminService.getPendingPosts(page, size);
    }

    @PostMapping("/posts/{postId}/approve")
    public ResponseEntity<Void> approvePendingPost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        requireAdmin(authentication);
        adminService.approvePendingPost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/reject")
    public ResponseEntity<Void> rejectPendingPost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        requireAdmin(authentication);
        adminService.rejectPendingPost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public AdminReportedPostFeedResponse getReportedPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        requireAdmin(authentication);
        return adminService.getReportedPosts(page, size);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deleteReportedPost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        requireAdmin(authentication);
        adminService.deleteReportedPost(postId, authentication.getName());
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

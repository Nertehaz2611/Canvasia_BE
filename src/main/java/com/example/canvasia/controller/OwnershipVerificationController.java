package com.example.canvasia.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.ownership.CreateOwnershipVerificationRequest;
import com.example.canvasia.dto.ownership.OwnershipVerificationResponse;
import com.example.canvasia.dto.ownership.ReviewOwnershipVerificationRequest;
import com.example.canvasia.service.interfaces.OwnershipVerificationModerationService;
import com.example.canvasia.service.interfaces.OwnershipVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ownership-verifications")
@RequiredArgsConstructor
public class OwnershipVerificationController {

    private final OwnershipVerificationService ownershipVerificationService;
    private final OwnershipVerificationModerationService ownershipVerificationModerationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OwnershipVerificationResponse> submitOwnershipVerification(
            Authentication authentication,
            @Valid @RequestBody CreateOwnershipVerificationRequest request
    ) {
        String userEmail = authentication.getName();

        OwnershipVerificationResponse response = ownershipVerificationService
                .submitOwnershipVerification(userEmail, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{verificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OwnershipVerificationResponse> getOwnershipVerification(
            @PathVariable UUID verificationId
    ) {
        OwnershipVerificationResponse response = ownershipVerificationService
                .getOwnershipVerificationById(verificationId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OwnershipVerificationResponse>> getUserOwnershipVerifications(
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        List<OwnershipVerificationResponse> responses = ownershipVerificationService
                .getUserOwnershipVerifications(userEmail);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OwnershipVerificationResponse>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<OwnershipVerificationResponse> responses = ownershipVerificationService
                .getPendingVerifications(pageable);

        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OwnershipVerificationResponse>> getAllVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OwnershipVerificationResponse> responses = ownershipVerificationService
                .getAllVerifications(pageable);

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{verificationId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OwnershipVerificationResponse> reviewOwnershipVerification(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @Valid @RequestBody ReviewOwnershipVerificationRequest request
    ) {
        String adminEmail = authentication.getName();

        OwnershipVerificationResponse response = ownershipVerificationService
                .reviewOwnershipVerification(verificationId, adminEmail, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-approved")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> hasApprovedOwnershipVerification(
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        boolean hasApproved = ownershipVerificationService.hasApprovedOwnershipVerification(userEmail);

        return ResponseEntity.ok(hasApproved);
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleOwnershipVerificationCallback(
            @RequestParam(required = false) UUID ownershipVerificationId,
            @RequestParam(required = false) UUID ownedPostId,
            @RequestParam String status,
            @RequestParam double similarity,
            @RequestParam(required = false) UUID matchedPostId,
            @RequestParam(required = false) UUID matchedMediaId
    ) {
        try {
            ownershipVerificationModerationService.handleOwnershipVerificationCallback(
                    ownershipVerificationId,
                    ownedPostId,
                    status,
                    similarity,
                    matchedPostId,
                    matchedMediaId
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling ownership verification callback", e);
            return ResponseEntity.status(500).build();
        }
    }
}

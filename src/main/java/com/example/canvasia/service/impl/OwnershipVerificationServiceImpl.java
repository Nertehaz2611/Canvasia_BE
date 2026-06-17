package com.example.canvasia.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.ownership.CreateOwnershipVerificationRequest;
import com.example.canvasia.dto.ownership.OwnershipVerificationResponse;
import com.example.canvasia.dto.ownership.OwnershipVerificationResponse.OwnershipVerificationMediaResponse;
import com.example.canvasia.dto.ownership.ReviewOwnershipVerificationRequest;
import com.example.canvasia.dto.ownership.ReviewOwnershipVerificationRequest.ReviewAction;
import com.example.canvasia.entity.OwnershipVerification;
import com.example.canvasia.entity.OwnershipVerificationMedia;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.OwnershipVerificationStatus;
import com.example.canvasia.enums.RejectionReason;
import com.example.canvasia.exception.DomainValidationException;
import com.example.canvasia.repository.OwnershipVerificationRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.OwnershipVerificationService;
import com.example.canvasia.service.interfaces.OwnershipVerificationModerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipVerificationServiceImpl implements OwnershipVerificationService {

    private final OwnershipVerificationRepository ownershipVerificationRepository;
    private final UserRepository userRepository;
    private final OwnershipVerificationModerationService ownershipVerificationModerationService;

    private static final String USER_NOT_FOUND_CODE = "USER_NOT_FOUND";
    private static final String USER_NOT_FOUND_MSG = "User not found";
    private static final String VERIFICATION_NOT_FOUND_CODE = "VERIFICATION_NOT_FOUND";
    private static final String VERIFICATION_NOT_FOUND_MSG = "Verification not found";
    private static final String ADMIN_NOT_FOUND_CODE = "ADMIN_NOT_FOUND";
    private static final String ADMIN_NOT_FOUND_MSG = "Admin not found";

    @Override
    @Transactional
    public OwnershipVerificationResponse submitOwnershipVerification(
            String username,
            CreateOwnershipVerificationRequest request
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainValidationException(USER_NOT_FOUND_CODE, USER_NOT_FOUND_MSG));

        // Create ownership verification
        OwnershipVerification verification = OwnershipVerification.create(
                user,
                request.getFullName(),
                request.getDateOfBirth(),
                request.getIdentityDocumentType(),
                request.getIdentityDocumentNumber(),
                request.getCountryCode()
        );

        // Add media files
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            int index = 0;
            for (CreateOwnershipVerificationRequest.OwnershipVerificationMediaRequest mediaRequest : request.getMediaFiles()) {
                Integer displayOrderFromRequest = mediaRequest.getDisplayOrder();
                int displayOrder = displayOrderFromRequest != null ? displayOrderFromRequest : index;
                OwnershipVerificationMedia media = OwnershipVerificationMedia.create(
                        mediaRequest.getMediaUrl(),
                        mediaRequest.getPublicId(),
                        mediaRequest.getMediaType(),
                        mediaRequest.getFileName(),
                        displayOrder
                );
                verification.addMedia(media);
                index++;
            }
        }

        OwnershipVerification savedVerification = ownershipVerificationRepository.save(verification);
        log.info("Ownership verification submitted by user: {}", username);

        return mapToResponse(savedVerification);
    }

    @Override
    public OwnershipVerificationResponse getOwnershipVerificationById(UUID verificationId) {
        OwnershipVerification verification = ownershipVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new DomainValidationException(VERIFICATION_NOT_FOUND_CODE, VERIFICATION_NOT_FOUND_MSG));

        return mapToResponse(verification);
    }

    @Override
    public List<OwnershipVerificationResponse> getUserOwnershipVerifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainValidationException(USER_NOT_FOUND_CODE, USER_NOT_FOUND_MSG));
        
        List<OwnershipVerification> verifications = ownershipVerificationRepository.findByUserId(user.getId());

        return verifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Page<OwnershipVerificationResponse> getPendingVerifications(Pageable pageable) {
        Page<OwnershipVerification> verifications = ownershipVerificationRepository.findByStatus(
                OwnershipVerificationStatus.PENDING,
                pageable
        );

        return verifications.map(this::mapToResponse);
    }

    @Override
    public Page<OwnershipVerificationResponse> getAllVerifications(Pageable pageable) {
        Page<OwnershipVerification> verifications = ownershipVerificationRepository.findAll(pageable);

        return verifications.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OwnershipVerificationResponse reviewOwnershipVerification(
            UUID verificationId,
            String adminUsername,
            ReviewOwnershipVerificationRequest request
    ) {
        OwnershipVerification verification = ownershipVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new DomainValidationException(VERIFICATION_NOT_FOUND_CODE, VERIFICATION_NOT_FOUND_MSG));

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new DomainValidationException(ADMIN_NOT_FOUND_CODE, ADMIN_NOT_FOUND_MSG));

        if (request.getAction() == ReviewAction.APPROVE) {
            verification.approve(admin);
            log.info("Ownership verification approved: {} by admin: {}", verificationId, adminUsername);
            
            // After saving, trigger AI processing
            OwnershipVerification savedVerification = ownershipVerificationRepository.save(verification);
            ownershipVerificationModerationService.triggerAiProcessing(savedVerification);
            return mapToResponse(savedVerification);
        } else if (request.getAction() == ReviewAction.REJECT) {
            RejectionReason reason = RejectionReason.OTHER;
            if (request.getRejectionReason() != null) {
                try {
                    reason = RejectionReason.valueOf(request.getRejectionReason());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid rejection reason: {}", request.getRejectionReason());
                }
            }
            verification.reject(admin, reason, request.getAdminNotes());
            log.info("Ownership verification rejected: {} by admin: {}", verificationId, adminUsername);
            
            OwnershipVerification savedVerification = ownershipVerificationRepository.save(verification);
            return mapToResponse(savedVerification);
        }

        OwnershipVerification savedVerification = ownershipVerificationRepository.save(verification);
        return mapToResponse(savedVerification);
    }

    @Override
    public boolean hasApprovedOwnershipVerification(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainValidationException(USER_NOT_FOUND_CODE, USER_NOT_FOUND_MSG));
        
        List<OwnershipVerification> approvedVerifications = ownershipVerificationRepository
                .findByStatusAndUserId(OwnershipVerificationStatus.APPROVED, user.getId());

        return !approvedVerifications.isEmpty();
    }

    @Override
    public OwnershipVerification findByPostId(UUID postId) {
        return ownershipVerificationRepository.findByPostId(postId).orElse(null);
    }

    @Override
    public List<OwnershipVerification> findApprovedVerificationsByUser(UUID userId) {
        return ownershipVerificationRepository.findByStatusAndUserId(
                OwnershipVerificationStatus.APPROVED,
                userId
        );
    }

    private OwnershipVerificationResponse mapToResponse(OwnershipVerification verification) {
        String adminName = null;
        if (verification.getReviewedByAdmin() != null) {
            adminName = verification.getReviewedByAdmin().getDisplayName();
        }

        return OwnershipVerificationResponse.builder()
                .id(verification.getId())
                .userId(verification.getUser().getId())
                .postId(verification.getPost() != null ? verification.getPost().getId() : null)
                .fullName(verification.getFullName())
                .dateOfBirth(verification.getDateOfBirth())
                .identityDocumentType(verification.getIdentityDocumentType())
                .identityDocumentNumber(verification.getIdentityDocumentNumber())
                .countryCode(verification.getCountryCode())
                .status(verification.getStatus().toString())
                .createdAt(verification.getCreatedAt())
                .reviewedAt(verification.getReviewedAt())
                .rejectionReason(verification.getRejectionReason() != null ? verification.getRejectionReason().toString() : null)
                .adminNotes(verification.getAdminNotes())
                .reviewedByAdminName(adminName)
                .mediaFiles(new HashSet<>(verification.getMediaFiles().stream()
                        .map(media -> OwnershipVerificationMediaResponse.builder()
                                .id(media.getId())
                                .mediaUrl(media.getMediaUrl())
                                .mediaType(media.getMediaType())
                                .fileName(media.getFileName())
                                .displayOrder(media.getDisplayOrder())
                                .createdAt(media.getCreatedAt())
                                .build())
                        .toList()))
                .build();
    }
}

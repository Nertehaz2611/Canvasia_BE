package com.example.canvasia.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.OwnershipVerification;
import com.example.canvasia.entity.OwnershipVerificationMedia;
import com.example.canvasia.entity.Post;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.OwnershipVerificationRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.service.interfaces.NotificationService;
import com.example.canvasia.service.interfaces.OwnershipVerificationModerationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipVerificationModerationServiceImpl implements OwnershipVerificationModerationService {

    private final RabbitTemplate rabbitTemplate;
    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;
    private final OwnershipVerificationRepository ownershipVerificationRepository;
    private final NotificationService notificationService;

    private static final String OWNERSHIP_VERIFICATION_QUEUE = "ownership_verification";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final double THRESHOLD = 0.70;

    @Override
    @Transactional
    public void triggerAiProcessing(OwnershipVerification verification) {
        List<OwnershipCandidateMedia> candidateMedia = new ArrayList<>();

        // Extract vectors only for registered artwork files, ignore support documents/proofs.
        for (OwnershipVerificationMedia media : verification.getMediaFiles()) {
            boolean isArtwork = media.getMediaType() != null && media.getMediaType().toUpperCase().startsWith("ARTWORK");
            boolean hasUrl = media.getMediaUrl() != null && !media.getMediaUrl().isBlank();
            if (isArtwork && hasUrl) {
                candidateMedia.add(new OwnershipCandidateMedia(media.getId().toString(), media.getMediaUrl(), media.getMediaType()));
            }
        }

        if (candidateMedia.isEmpty()) {
            log.warn("Cannot trigger AI processing: ownership verification {} has no image media", verification.getId());
            return;
        }

        for (OwnershipCandidateMedia media : candidateMedia) {
            try {
                OwnershipVerificationAiRequest aiRequest = OwnershipVerificationAiRequest.builder()
                        .verificationId(verification.getId().toString())
                        .postId(verification.getPost() != null ? verification.getPost().getId().toString() : null)
                        .mediaId(media.mediaId())
                        .mediaUrl(media.mediaUrl())
                        .mediaType(media.mediaType())
                        .userId(verification.getUser().getId().toString())
                        .build();

                ObjectMapper objectMapper = new ObjectMapper();
                String payload = objectMapper.writeValueAsString(aiRequest);
                rabbitTemplate.convertAndSend(OWNERSHIP_VERIFICATION_QUEUE, payload);

                log.info("Sent AI processing request for ownership verification: {} media: {}",
                        verification.getId(), media.mediaId());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize ownership AI request for media: {}", media.mediaId(), e);
            } catch (RuntimeException e) {
                log.error("Failed to send AI processing request for media: {}", media.mediaId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void handleOwnershipVerificationCallback(
            UUID ownershipVerificationId,
            UUID ownedPostId,
            String status,
            double similarity,
            UUID matchedPostId,
            UUID matchedMediaId
    ) {
        log.info("[Ownership Callback] Received: ownershipVerificationId={}, ownedPostId={}, matchedPostId={}, matchedMediaId={}, similarity={}",
                ownershipVerificationId, ownedPostId, matchedPostId, matchedMediaId, similarity);

        if (!"PENDING".equalsIgnoreCase(status)) {
            return;
        }

        UUID violatingPostId = matchedPostId;
        if (violatingPostId == null && matchedMediaId != null) {
            log.info("[Ownership Callback] matchedMediaId: {} trying to find in Media repository...", matchedMediaId);
            Media matchedMedia = mediaRepository.findByIdWithPostAndUser(matchedMediaId).orElse(null);
            if (matchedMedia != null) {
                log.info("[Ownership Callback] Found media: {}, post: {}, post.user: {}", 
                        matchedMediaId, matchedMedia.getPost() != null ? matchedMedia.getPost().getId() : null,
                        matchedMedia.getPost() != null ? matchedMedia.getPost().getUser() : null);
            }
            if (matchedMedia != null && matchedMedia.getPost() != null) {
                violatingPostId = matchedMedia.getPost().getId();
                log.info("[Ownership Callback] Extracted violatingPostId from media: {}", violatingPostId);
            } else {
                log.warn("[Ownership Callback] Media not found or no post association, matchedMediaId={}", matchedMediaId);
            }
        }

        if (violatingPostId == null) {
            log.warn("[Ownership Callback] Violating post ID could not be determined");
            return;
        }

        Post violatingPost = postRepository.findByIdWithUserEager(violatingPostId).orElse(null);
        if (violatingPost == null || Boolean.TRUE.equals(violatingPost.getIsDeleted())) {
            log.warn("[Ownership Callback] Violating post not found or deleted: {}", violatingPostId);
            return;
        }

        // Query user_id directly from database to avoid lazy loading issues
        UUID violatingPostUserId = postRepository.findUserIdByPostId(violatingPostId).orElse(null);
        log.info("[Ownership Callback] Found post: {}, user_id (from DB): {}",
                violatingPostId, violatingPostUserId);

        if (violatingPostUserId == null) {
            log.error("[Ownership Callback] CRITICAL: Could not find user_id for post: {}", violatingPostId);
            return;
        }

        // Find the approved ownership verification
        OwnershipVerification ownedVerification = null;
        UUID ownedUserId = null;
        
        if (ownershipVerificationId != null) {
            ownedVerification = ownershipVerificationRepository.findById(ownershipVerificationId).orElse(null);
            if (ownedVerification != null && APPROVED_STATUS.equals(ownedVerification.getStatus().name())) {
                // Query user_id directly from database to avoid lazy loading issues
                ownedUserId = ownershipVerificationRepository.findUserIdByVerificationId(ownershipVerificationId).orElse(null);
                log.info("[Ownership Callback] Found OV by verificationId {}: status={}, user_id (from DB)={}",
                        ownershipVerificationId, ownedVerification.getStatus(), ownedUserId);
            } else {
                log.warn("[Ownership Callback] Ownership verification not found or not approved by id: {}", ownershipVerificationId);
            }
        }

        if (ownedVerification == null && ownedPostId != null) {
            ownedVerification = ownershipVerificationRepository.findByPostId(ownedPostId).orElse(null);
            if (ownedVerification != null && APPROVED_STATUS.equals(ownedVerification.getStatus().name())) {
                // Query user_id directly from database
                ownedUserId = ownershipVerificationRepository.findUserIdByPostId(ownedPostId).orElse(null);
                log.info("[Ownership Callback] Found OV by postId {}: status={}, user_id (from DB)={}",
                        ownedPostId, ownedVerification.getStatus(), ownedUserId);
            } else {
                log.warn("[Ownership Callback] Ownership verification not found or not approved by postId: {}", ownedPostId);
            }
        }

        if (ownedVerification == null || ownedUserId == null) {
            log.warn("[Ownership Callback] Missing OV or user_id: ov={}, userId={}", ownedVerification != null, ownedUserId);
            return;
        }

        log.info("[Ownership Callback] Using ownedUserId={} for comparison", ownedUserId);

        // Check if matched post already has approved ownership registration
        OwnershipVerification violatingVerification = ownershipVerificationRepository.findByPostId(violatingPostId).orElse(null);
        if (violatingVerification != null && APPROVED_STATUS.equals(violatingVerification.getStatus().name())) {
            log.info("Skipped copyright violation marking: matched post {} already has approved ownership verification", violatingPostId);
            return;
        }

        // Check if same user (owner is re-uploading their own work)
        log.info("[Ownership Callback] Comparing users: violatingPostUserId={}, ownedUserId={}", violatingPostUserId, ownedUserId);

        if (violatingPostUserId.equals(ownedUserId)) {
            log.info("[Ownership Callback] SKIPPED: Same owner re-upload (userId={}) - no pending flag", violatingPostUserId);
            return;
        }

        log.info("[Ownership Callback] DIFFERENT users detected - will flag post as pending");

        UUID referencePostId = ownedVerification.getPost() != null
            ? ownedVerification.getPost().getId()
            : ownedPostId;
        
        log.info("[Ownership Callback] Using referencePostId: {}, will flag violatingPost: {} by user: {} with flaggedMatchedPostId", 
                referencePostId, violatingPostId, violatingPostUserId);

        // Mark post as pending for copyright violation and keep source metadata for admin audit.
        violatingPost.markPending(true);
        violatingPost.flagWith(referencePostId, ownedVerification.getUser().getDisplayName());
        postRepository.save(violatingPost);

        log.info("[Copyright Violation] Post {} marked PENDING (copyright violation detected, similarity={})",
            violatingPostId, similarity);

        // Notify the violating post owner
        notificationService.notifyPostPending(violatingPost);
    }

    @Override
    public boolean checkOwnershipMatch(UUID postId, double similarity, UUID approvedOwnershipPostId) {
        if (similarity < THRESHOLD) {
            return false;
        }

        // Check if the approvedOwnershipPostId has an approved ownership verification
        OwnershipVerification verification = ownershipVerificationRepository.findByPostId(approvedOwnershipPostId)
                .orElse(null);

        return verification != null && APPROVED_STATUS.equals(verification.getStatus().name());
    }

    // DTO for AI request
    @lombok.Builder
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OwnershipVerificationAiRequest {
        private String verificationId;
        private String postId;
        private String mediaId;
        private String mediaUrl;
        private String mediaType;
        private String userId;
        
        @lombok.Builder.Default
        private String callbackUrl = "http://localhost:8081/api/ownership-verifications/callback";
    }

    private record OwnershipCandidateMedia(String mediaId, String mediaUrl, String mediaType) {
    }
}

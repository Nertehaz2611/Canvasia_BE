package com.example.canvasia.service.interfaces;

import java.util.UUID;

import com.example.canvasia.entity.OwnershipVerification;

public interface OwnershipVerificationModerationService {

    /**
     * Trigger AI to extract vectors for approved ownership verification
     * This is called after admin approves an ownership verification
     */
    void triggerAiProcessing(OwnershipVerification verification);

    /**
     * Handle callback from AI server for ownership verification
     * Updates post status based on similarity results
     */
        void handleOwnershipVerificationCallback(
            UUID ownershipVerificationId,
            UUID ownedPostId,
            String status,
            double similarity,
                UUID matchedPostId,
                UUID matchedMediaId
        );

    /**
     * Check if a post matches any approved ownership verifications
     * Returns true if similarity >= threshold with approved ownership post
     */
    boolean checkOwnershipMatch(UUID postId, double similarity, UUID approvedOwnershipPostId);
}

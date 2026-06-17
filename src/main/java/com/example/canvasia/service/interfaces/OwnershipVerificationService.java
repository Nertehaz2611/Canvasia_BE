package com.example.canvasia.service.interfaces;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.canvasia.dto.ownership.CreateOwnershipVerificationRequest;
import com.example.canvasia.dto.ownership.OwnershipVerificationResponse;
import com.example.canvasia.dto.ownership.ReviewOwnershipVerificationRequest;
import com.example.canvasia.entity.OwnershipVerification;
import com.example.canvasia.enums.OwnershipVerificationStatus;

public interface OwnershipVerificationService {

    OwnershipVerificationResponse submitOwnershipVerification(String userEmail, CreateOwnershipVerificationRequest request);

    OwnershipVerificationResponse getOwnershipVerificationById(UUID verificationId);

    List<OwnershipVerificationResponse> getUserOwnershipVerifications(String userEmail);

    Page<OwnershipVerificationResponse> getPendingVerifications(Pageable pageable);

    Page<OwnershipVerificationResponse> getAllVerifications(Pageable pageable);

    OwnershipVerificationResponse reviewOwnershipVerification(UUID verificationId, String adminEmail, ReviewOwnershipVerificationRequest request);

    boolean hasApprovedOwnershipVerification(String userEmail);

    OwnershipVerification findByPostId(UUID postId);

    List<OwnershipVerification> findApprovedVerificationsByUser(UUID userId);
}

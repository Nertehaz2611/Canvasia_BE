package com.example.canvasia.dto.ownership;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipVerificationResponse {

    private UUID id;
    private UUID userId;
    private UUID postId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String identityDocumentType;
    private String identityDocumentNumber;
    private String countryCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private String adminNotes;
    private String reviewedByAdminName;
    private Set<OwnershipVerificationMediaResponse> mediaFiles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnershipVerificationMediaResponse {
        private UUID id;
        private String mediaUrl;
        private String mediaType;
        private String fileName;
        private Integer displayOrder;
        private LocalDateTime createdAt;
    }
}

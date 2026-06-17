package com.example.canvasia.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.enums.OwnershipVerificationStatus;
import com.example.canvasia.enums.RejectionReason;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "ownership_verifications",
        indexes = {
                @Index(columnList = "user_id"),
                @Index(columnList = "status"),
                @Index(columnList = "created_at")
        }
)
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class OwnershipVerification extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", unique = true)
    @ToString.Exclude
    private Post post;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 50)
    private String identityDocumentType; // e.g., "PASSPORT", "ID_CARD", "DRIVER_LICENSE"

    @Column(nullable = false, length = 50)
    private String identityDocumentNumber;

    @Column(nullable = false, length = 50)
    private String countryCode; // e.g., "VN", "US", "JP"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OwnershipVerificationStatus status = OwnershipVerificationStatus.PENDING;

    @OneToMany(mappedBy = "ownershipVerification", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<OwnershipVerificationMedia> mediaFiles = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    @Column
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    @ToString.Exclude
    private User reviewedByAdmin;

    public static OwnershipVerification create(
            User user,
            String fullName,
            LocalDate dateOfBirth,
            String identityDocumentType,
            String identityDocumentNumber,
            String countryCode
    ) {
        return OwnershipVerification.builder()
                .user(user)
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .identityDocumentType(identityDocumentType)
                .identityDocumentNumber(identityDocumentNumber)
                .countryCode(countryCode)
                .status(OwnershipVerificationStatus.PENDING)
                .build();
    }

    public void addMedia(OwnershipVerificationMedia media) {
        if (this.mediaFiles == null) {
            this.mediaFiles = new HashSet<>();
        }
        media.setOwnershipVerification(this);
        this.mediaFiles.add(media);
    }

    public void approve(User admin) {
        this.status = OwnershipVerificationStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByAdmin = admin;
    }

    public void reject(User admin, RejectionReason reason, String notes) {
        this.status = OwnershipVerificationStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByAdmin = admin;
        this.rejectionReason = reason;
        this.adminNotes = notes;
    }
}

package com.example.canvasia.entity;

import java.util.UUID;

import com.example.canvasia.entity.base.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "ownership_verification_media",
        indexes = {
                @Index(columnList = "ownership_verification_id")
        }
)
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class OwnershipVerificationMedia extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_verification_id", nullable = false)
    @ToString.Exclude
    private OwnershipVerification ownershipVerification;

    @Column(nullable = false, length = 500)
    private String mediaUrl; // Cloudinary URL

    @Column(nullable = false, length = 100)
    private String publicId; // Cloudinary public ID for deletion

    @Column(nullable = false, length = 20)
    private String mediaType; // e.g., "IMAGE", "PDF"

    @Column
    private String fileName;

    @Column
    private Integer displayOrder;

    public static OwnershipVerificationMedia create(
            String mediaUrl,
            String publicId,
            String mediaType,
            String fileName,
            Integer displayOrder
    ) {
        return OwnershipVerificationMedia.builder()
                .mediaUrl(mediaUrl)
                .publicId(publicId)
                .mediaType(mediaType)
                .fileName(fileName)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();
    }
}

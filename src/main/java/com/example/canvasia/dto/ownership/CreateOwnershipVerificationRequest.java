package com.example.canvasia.dto.ownership;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class CreateOwnershipVerificationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Identity document type is required")
    private String identityDocumentType; // PASSPORT, ID_CARD, DRIVER_LICENSE, etc.

    @NotBlank(message = "Identity document number is required")
    private String identityDocumentNumber;

    @NotBlank(message = "Country code is required")
    private String countryCode; // VN, US, JP, etc.

    @NotNull(message = "Media files are required")
    private List<OwnershipVerificationMediaRequest> mediaFiles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnershipVerificationMediaRequest {
        @NotBlank(message = "Media URL is required")
        private String mediaUrl;

        @NotBlank(message = "Public ID is required")
        private String publicId;

        @NotBlank(message = "Media type is required")
        private String mediaType; // IMAGE, PDF

        private String fileName;

        private Integer displayOrder;
    }
}

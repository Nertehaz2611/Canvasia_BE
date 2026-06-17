package com.example.canvasia.dto.ownership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ReviewOwnershipVerificationRequest {

    @NotNull(message = "Action is required (APPROVE or REJECT)")
    private ReviewAction action;

    private String rejectionReason;

    private String adminNotes;

    public enum ReviewAction {
        APPROVE, REJECT
    }
}

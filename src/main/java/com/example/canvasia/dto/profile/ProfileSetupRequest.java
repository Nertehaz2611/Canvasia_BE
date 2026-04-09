package com.example.canvasia.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileSetupRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 25, message = "Display name must not exceed 25 characters")
    private String displayName;

    @Size(max = 300, message = "Bio must not exceed 300 characters")
    private String bio;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;
}

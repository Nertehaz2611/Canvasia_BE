package com.example.canvasia.dto.profile;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private UUID userId;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarPublicId;
    private String avatarUrl;
    private String website;
}

package com.example.canvasia.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvatarUploadResponse {
    private String avatarPublicId;
    private String avatarUrl;
}

package com.example.canvasia.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.profile.AvatarUploadResponse;
import com.example.canvasia.dto.profile.ProfileResponse;
import com.example.canvasia.dto.profile.ProfileSetupRequest;

public interface ProfileService {

    ProfileResponse getCurrentProfile(String username);

    ProfileResponse setupProfile(String username, ProfileSetupRequest request);

    AvatarUploadResponse uploadAvatar(String username, MultipartFile file);
}

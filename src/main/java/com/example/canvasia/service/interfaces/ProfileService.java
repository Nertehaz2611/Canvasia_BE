package com.example.canvasia.service.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.profile.AccountSettingsRequest;
import com.example.canvasia.dto.profile.AvatarUploadResponse;
import com.example.canvasia.dto.profile.ProfileResponse;
import com.example.canvasia.dto.profile.ProfileSetupRequest;

public interface ProfileService {

    ProfileResponse getCurrentProfile(String username);

    ProfileResponse getProfileByUsername(String viewerUsername, String username);

    List<ProfileResponse> searchProfiles(String viewerUsername, String query, int limit);

    ProfileResponse setupProfile(String username, ProfileSetupRequest request);

    ProfileResponse updateAccountSettings(String username, AccountSettingsRequest request);

    AvatarUploadResponse uploadAvatar(String username, MultipartFile file);
}

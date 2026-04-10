package com.example.canvasia.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.profile.AvatarUploadResponse;
import com.example.canvasia.dto.profile.ProfileResponse;
import com.example.canvasia.dto.profile.ProfileSetupRequest;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.AvatarStorageService;
import com.example.canvasia.service.interfaces.ProfileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AvatarStorageService avatarStorageService;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(String username) {
        User user = getUserByUsername(username);
        Profile profile = findProfileOrBuild(user);
        return toProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public ProfileResponse setupProfile(String username, ProfileSetupRequest request) {
        User user = getUserByUsername(username);
        Profile profile = ensureProfileForWrite(user);
        String normalizedDisplayName = request.getDisplayName().trim();

        profile.updateDisplayName(normalizedDisplayName);
        profile.updateBio(normalizeBlank(request.getBio()));
        profile.updateWebsite(normalizeBlank(request.getWebsite()));
        user.setDisplayName(normalizedDisplayName);

        profileRepository.save(profile);
        userRepository.save(user);

        return toProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(String username, MultipartFile file) {
        User user = getUserByUsername(username);
        Profile profile = ensureProfileForWrite(user);

        String avatarPublicId = avatarStorageService.uploadAvatar(file, user.getUsername(), user.getId().toString());
        String avatarUrl = avatarStorageService.buildDeliveryUrl(avatarPublicId);
        profile.updateAvatarPublicId(avatarPublicId);
        profile.updateAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        return new AvatarUploadResponse(avatarPublicId, avatarUrl);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Profile ensureProfileForWrite(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    if (user.getProfile() != null) {
                        return user.getProfile();
                    }
                    Profile profile = Profile.create(user, user.getDisplayName());
                    user.setProfile(profile);
                    return profileRepository.save(profile);
                });
    }

    private Profile findProfileOrBuild(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.create(user, user.getDisplayName()));
    }

    private ProfileResponse toProfileResponse(User user, Profile profile) {
        String avatarUrl = profile.getAvatarPublicId() != null
            ? avatarStorageService.buildDeliveryUrl(profile.getAvatarPublicId())
            : profile.getAvatarUrl();

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getDisplayName(),
                profile.getBio(),
            profile.getAvatarPublicId(),
            avatarUrl,
                profile.getWebsite()
        );
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

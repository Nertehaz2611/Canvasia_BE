package com.example.canvasia.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.profile.AccountSettingsRequest;
import com.example.canvasia.dto.profile.AvatarUploadResponse;
import com.example.canvasia.dto.profile.ProfileResponse;
import com.example.canvasia.dto.profile.ProfileSetupRequest;
import com.example.canvasia.service.interfaces.ProfileService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile(Authentication authentication) {
        return profileService.getCurrentProfile(authentication.getName());
    }

    @GetMapping("/users/{username}")
    public ProfileResponse getProfileByUsername(
            Authentication authentication,
            @PathVariable String username
    ) {
        return profileService.getProfileByUsername(authentication.getName(), username);
    }

    @PutMapping("/setup")
    public ProfileResponse setupProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileSetupRequest request
    ) {
        return profileService.setupProfile(authentication.getName(), request);
    }

    @PutMapping("/account")
    public ProfileResponse updateAccountSettings(
            Authentication authentication,
            @Valid @RequestBody AccountSettingsRequest request
    ) {
        return profileService.updateAccountSettings(authentication.getName(), request);
    }

    @PostMapping("/avatar")
    public AvatarUploadResponse uploadAvatar(
            Authentication authentication,
            @RequestParam("avatar") MultipartFile avatar
    ) {
        return profileService.uploadAvatar(authentication.getName(), avatar);
    }
}

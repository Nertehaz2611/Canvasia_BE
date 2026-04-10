package com.example.canvasia.service.impl;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.canvasia.service.interfaces.AvatarStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryAvatarStorageService implements AvatarStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_USERNAME_PART_LENGTH = 12;
    private static final int USER_ID_PART_LENGTH = 6;

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:canvasia/avatars}")
    private String uploadFolder;

    @Value("${cloudinary.avatar.delivery-width:512}")
    private Integer deliveryWidth;

    @Override
    public String uploadAvatar(MultipartFile file, String username, String userId) {
        validateFile(file);
        String ownerAwarePublicId = buildOwnerAwarePublicId(username, userId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", uploadFolder,
                            "resource_type", "image",
                            "public_id", ownerAwarePublicId,
                            "overwrite", true
                    )
            );

            Object publicIdObject = uploadResult.get("public_id");
            if (publicIdObject == null) {
                throw new IllegalStateException("Cloudinary upload succeeded but public_id was not returned");
            }

            return publicIdObject.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload avatar to Cloudinary", ex);
        }
    }

    @Override
    public String buildDeliveryUrl(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }

        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation<>()
                        .fetchFormat("auto")
                        .quality("auto")
                        .width(deliveryWidth)
                        .crop("limit"))
                .generate(publicId);
    }

    private String buildOwnerAwarePublicId(String username, String userId) {
        String normalizedUsername = normalizeUsername(username);
        String shortUserId = normalizeUserId(userId);
        return "avatar-" + normalizedUsername + "-" + shortUserId;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "user";
        }

        String normalized = username
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[.-]+|[.-]+$", "");

        if (normalized.isBlank()) {
            return "user";
        }

        return normalized.length() <= MAX_USERNAME_PART_LENGTH
                ? normalized
                : normalized.substring(0, MAX_USERNAME_PART_LENGTH);
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "000000";
        }

        String compact = userId.replace("-", "");
        if (compact.isBlank()) {
            return "000000";
        }

        return compact.length() <= USER_ID_PART_LENGTH
                ? compact
                : compact.substring(0, USER_ID_PART_LENGTH);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Avatar must be <= 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image file");
        }
    }
}

package com.example.canvasia.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {
    String uploadAvatar(MultipartFile file, String username, String userId);

    String buildDeliveryUrl(String publicId);
}

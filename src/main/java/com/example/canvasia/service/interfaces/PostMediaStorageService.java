package com.example.canvasia.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface PostMediaStorageService {

    UploadResult uploadImage(MultipartFile file, String userId, String postId, int index, CropArea cropArea);

        void deleteByPublicIds(Iterable<String> publicIds);

    record CropArea(
            int x,
            int y,
            int width,
            int height
    ) {
    }

    record UploadResult(
            String originalPublicId,
            String originalUrl,
            String thumbnailPublicId,
            String thumbnailUrl
    ) {
    }
}

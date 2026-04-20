package com.example.canvasia.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.canvasia.service.interfaces.PostMediaStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryPostMediaStorageService implements PostMediaStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final String PUBLIC_ID_KEY = "public_id";
    private static final String RESOURCE_TYPE_KEY = "resource_type";
    private static final String IMAGE_RESOURCE_TYPE = "image";

    private final Cloudinary cloudinary;

    @Value("${cloudinary.post.original-folder:canvasia/original}")
    private String originalFolder;

    @Value("${cloudinary.post.thumbnail-folder:canvasia/thumbnail}")
    private String thumbnailFolder;

    @Override
    public UploadResult uploadImage(MultipartFile file, String userId, String postId, int index, CropArea cropArea) {
        validateFile(file);
        String publicId = userId + "_" + postId + "_" + index;

        try {
            byte[] bytes = file.getBytes();
            ImageDimensions dimensions = readImageDimensions(bytes);
            Transformation<?> thumbnailTransformation = buildThumbnailTransformation(dimensions, cropArea);

            @SuppressWarnings("unchecked")
            Map<String, Object> originalResult = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", originalFolder,
                            RESOURCE_TYPE_KEY, IMAGE_RESOURCE_TYPE,
                            PUBLIC_ID_KEY, publicId,
                            "overwrite", true
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> thumbnailResult = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", thumbnailFolder,
                            RESOURCE_TYPE_KEY, IMAGE_RESOURCE_TYPE,
                            PUBLIC_ID_KEY, publicId,
                            "overwrite", true,
                            "transformation", thumbnailTransformation
                    )
            );

                    String originalPublicId = readRequired(originalResult, PUBLIC_ID_KEY);
                    String thumbnailPublicId = readRequired(thumbnailResult, PUBLIC_ID_KEY);
            String originalUrl = buildAutoFormatUrl(originalPublicId);
            String thumbnailUrl = buildAutoFormatUrl(thumbnailPublicId);

            return new UploadResult(originalPublicId, originalUrl, thumbnailPublicId, thumbnailUrl);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload post media to Cloudinary", ex);
        }
    }

    @Override
    public void deleteByPublicIds(Iterable<String> publicIds) {
        if (publicIds == null) {
            return;
        }

        Set<String> ids = new LinkedHashSet<>();
        for (String publicId : publicIds) {
            if (publicId != null && !publicId.isBlank()) {
                ids.add(publicId);
            }
        }
        if (ids.isEmpty()) {
            return;
        }

        try {
            for (String publicId : ids) {
                cloudinary.uploader().destroy(
                        publicId,
                        ObjectUtils.asMap(
                            RESOURCE_TYPE_KEY, IMAGE_RESOURCE_TYPE,
                            "invalidate", true
                        )
                );
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete post media from Cloudinary", ex);
        }
    }

    private ImageDimensions readImageDimensions(byte[] bytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("Media must be a valid image file");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        }
    }

    private Transformation<?> buildThumbnailTransformation(ImageDimensions dimensions, CropArea cropArea) {
        if (cropArea == null) {
            int cropSize = Math.min(dimensions.width(), dimensions.height());
            return new Transformation<>()
                    .width(cropSize)
                    .height(cropSize)
                    .crop("fill")
                    .gravity("auto")
                    .fetchFormat("auto")
                    .quality("auto");
        }

        validateCropBounds(dimensions, cropArea);
        return new Transformation<>()
                .x(cropArea.x())
                .y(cropArea.y())
                .width(cropArea.width())
                .height(cropArea.height())
                .crop("crop")
                .fetchFormat("auto")
                .quality("auto");
    }

    private void validateCropBounds(ImageDimensions dimensions, CropArea cropArea) {
        int right = cropArea.x() + cropArea.width();
        int bottom = cropArea.y() + cropArea.height();
        if (right > dimensions.width() || bottom > dimensions.height()) {
            throw new IllegalArgumentException("thumbnail crop area is outside image bounds");
        }
    }

    private String readRequired(Map<String, Object> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("Cloudinary upload succeeded but " + key + " was missing");
        }
        return value.toString();
    }

        private String buildAutoFormatUrl(String publicId) {
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation<>()
                        .quality("auto")
                .fetchFormat("auto"))
                .generate(publicId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Media file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Each media file must be <= 20MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are supported for post media");
        }
    }

    private record ImageDimensions(int width, int height) {
    }
}

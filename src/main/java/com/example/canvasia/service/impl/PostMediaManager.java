package com.example.canvasia.service.impl;

import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Post;
import com.example.canvasia.enums.MediaType;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.MediaVariantRepository;
import com.example.canvasia.service.interfaces.PostMediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostMediaManager {

    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final PostMediaStorageService postMediaStorageService;

    public void createMedia(
            Post post,
            MultipartFile file,
            int orderIndex,
            PostMediaStorageService.CropArea cropArea
    ) {
        Media media = mediaRepository.save(Media.create(post, MediaType.IMAGE, orderIndex));
        PostMediaStorageService.UploadResult uploadResult = postMediaStorageService.uploadImage(
                file,
                post.getUser().getId().toString(),
                post.getId().toString(),
                orderIndex,
                cropArea
        );

        mediaVariantRepository.save(MediaVariant.create(
                uploadResult.originalPublicId(),
                uploadResult.originalUrl(),
                MediaVariantType.ORIGINAL,
                media
        ));
        mediaVariantRepository.save(MediaVariant.create(
                uploadResult.thumbnailPublicId(),
                uploadResult.thumbnailUrl(),
                MediaVariantType.THUMBNAIL,
                media
        ));
    }

    public List<Media> findByPostIdOrdered(UUID postId) {
        return mediaRepository.findByPostIdOrderByOrderIndexAsc(postId);
    }

    public void deleteMediaAndAssets(List<Media> mediaToDelete) {
        if (mediaToDelete == null || mediaToDelete.isEmpty()) {
            return;
        }

        List<UUID> mediaIds = mediaToDelete.stream().map(Media::getId).toList();
        List<MediaVariant> variants = mediaVariantRepository.findByMediaIdIn(mediaIds);
        postMediaStorageService.deleteByPublicIds(variants.stream().map(MediaVariant::getPublicId).toList());
        mediaVariantRepository.deleteByMediaIdIn(mediaIds);
        mediaRepository.deleteAll(mediaToDelete);
    }

    public void normalizeMediaOrder(UUID postId) {
        List<Media> media = findByPostIdOrdered(postId);
        for (int index = 0; index < media.size(); index++) {
            media.get(index).updateOrder(index);
        }
        if (!media.isEmpty()) {
            mediaRepository.saveAll(media);
        }
    }

    public int nextOrderIndex(UUID postId) {
        return findByPostIdOrdered(postId).stream()
                .map(Media::getOrderIndex)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    public List<MediaItemResponse> buildOriginalMediaResponses(UUID postId) {
        List<Media> media = findByPostIdOrdered(postId);
        if (media.isEmpty()) {
            return List.of();
        }

        List<UUID> mediaIds = media.stream().map(Media::getId).toList();
        Map<UUID, MediaVariant> originalVariantsByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.ORIGINAL)
                .stream()
                .collect(Collectors.toMap(variant -> variant.getMedia().getId(), variant -> variant));

        List<MediaItemResponse> responses = new ArrayList<>();
        for (Media item : media) {
            MediaVariant originalVariant = originalVariantsByMediaId.get(item.getId());
            if (originalVariant == null) {
                throw new IllegalStateException("Original media variant is missing for media " + item.getId());
            }

            responses.add(new MediaItemResponse(
                    item.getId(),
                    item.getOrderIndex(),
                    originalVariant.getPublicId(),
                    originalVariant.getUrl()
            ));
        }

        return responses;
    }
}

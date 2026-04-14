package com.example.canvasia.service.impl.media;

import com.example.canvasia.dto.media.MediaQueryItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.MediaVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MediaQueryAssembler {

    private final MediaVariantRepository mediaVariantRepository;

    public List<MediaQueryItemResponse> toMediaItems(List<Media> mediaList) {
        if (mediaList.isEmpty()) {
            return List.of();
        }

        List<UUID> mediaIds = mediaList.stream().map(Media::getId).toList();
        List<MediaVariant> variants = mediaVariantRepository.findByMediaIdIn(mediaIds);

        Map<UUID, MediaVariant> originalByMediaId = variants.stream()
                .filter(variant -> variant.getType() == MediaVariantType.ORIGINAL)
                .collect(Collectors.toMap(variant -> variant.getMedia().getId(), Function.identity(), (left, right) -> left));

        Map<UUID, MediaVariant> thumbnailByMediaId = variants.stream()
                .filter(variant -> variant.getType() == MediaVariantType.THUMBNAIL)
                .collect(Collectors.toMap(variant -> variant.getMedia().getId(), Function.identity(), (left, right) -> left));

        return mediaList.stream()
                .map(media -> {
                    MediaVariant original = originalByMediaId.get(media.getId());
                    MediaVariant thumbnail = thumbnailByMediaId.get(media.getId());
                    return new MediaQueryItemResponse(
                            media.getId(),
                            media.getPost().getId(),
                            media.getUserId(),
                            media.getOrderIndex(),
                            original != null ? original.getPublicId() : null,
                            original != null ? original.getUrl() : null,
                            thumbnail != null ? thumbnail.getPublicId() : null,
                            thumbnail != null ? thumbnail.getUrl() : null
                    );
                })
                .toList();
    }
}

package com.example.canvasia.service.impl.post;

import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.ThumbnailItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Post;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.MediaVariantRepository;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostFeedAssembler {

    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final PostTagRepository postTagRepository;
        private final PostLikeRepository postLikeRepository;

        public List<PostResponse> toPostResponses(List<Post> posts, String viewerUsername) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        List<Media> mediaList = mediaRepository.findByPostIdInOrderByOrderIndexAsc(postIds);
        Map<UUID, List<Media>> mediaByPostId = mediaList.stream()
                .collect(Collectors.groupingBy(media -> media.getPost().getId(), LinkedHashMap::new, Collectors.toList()));

        List<UUID> mediaIds = mediaList.stream().map(Media::getId).toList();

        Map<UUID, MediaVariant> originalByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.ORIGINAL)
                .stream()
                .collect(Collectors.toMap(variant -> variant.getMedia().getId(), variant -> variant));

        Map<UUID, List<String>> tagsByPostId = postTagRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        postTag -> postTag.getPost().getId(),
                        Collectors.mapping(postTag -> postTag.getTag().getName(), Collectors.toList())
                ));

        Map<UUID, Long> likeCountByPostId = new HashMap<>();
        for (PostLikeRepository.PostLikeCountView row : postLikeRepository.countByPostIds(postIds)) {
            likeCountByPostId.put(row.getPostId(), row.getLikeCount());
        }

        Set<UUID> likedPostIds = new HashSet<>();
        if (viewerUsername != null && !viewerUsername.isBlank()) {
            likedPostIds.addAll(postLikeRepository.findLikedPostIdsByUsernameAndPostIds(viewerUsername, postIds));
        }

        return posts.stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getUser().getId(),
                        post.getUser().getDisplayName(),
                        post.getUser().getUsername(),
                        post.getCaption(),
                        post.getCreatedAt(),
                        toMediaResponses(mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()), originalByMediaId),
                        tagsByPostId.getOrDefault(post.getId(), List.of()),
                        likeCountByPostId.getOrDefault(post.getId(), 0L),
                        likedPostIds.contains(post.getId())
                ))
                .toList();
    }

    public ThumbnailItemResponse toThumbnailItemResponse(MediaVariant variant) {
        Media media = variant.getMedia();
        Post post = media.getPost();

        return new ThumbnailItemResponse(
                media.getId(),
                post.getId(),
                media.getUserId(),
                media.getOrderIndex(),
                variant.getPublicId(),
                variant.getUrl()
        );
    }

    private List<MediaItemResponse> toMediaResponses(
            List<Media> mediaList,
            Map<UUID, MediaVariant> originalByMediaId
    ) {
        return mediaList.stream()
                .map(media -> {
                    MediaVariant original = originalByMediaId.get(media.getId());
                    return new MediaItemResponse(
                            media.getId(),
                            media.getOrderIndex(),
                            original != null ? original.getPublicId() : null,
                            original != null ? original.getUrl() : null
                    );
                })
                .toList();
    }
}

package com.example.canvasia.service.impl.post;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.ThumbnailItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.MediaVariantRepository;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostFeedAssembler {

    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final PostTagRepository postTagRepository;
        private final PostLikeRepository postLikeRepository;
        private final CommentRepository commentRepository;
        private final ProfileRepository profileRepository;

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

        Map<UUID, MediaVariant> thumbnailByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.THUMBNAIL)
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

        Map<UUID, Long> commentCountByPostId = new HashMap<>();
        for (CommentRepository.PostCommentCountView row : commentRepository.countByPostIds(postIds)) {
            commentCountByPostId.put(row.getPostId(), row.getCommentCount());
        }

        Set<UUID> likedPostIds = new HashSet<>();
        if (viewerUsername != null && !viewerUsername.isBlank()) {
            likedPostIds.addAll(postLikeRepository.findLikedPostIdsByUsernameAndPostIds(viewerUsername, postIds));
        }

        Map<UUID, String> avatarUrlByUserId = loadAvatarUrls(posts.stream().map(post -> post.getUser().getId()).toList());

        return posts.stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getUser().getId(),
                        post.getUser().getDisplayName(),
                        post.getUser().getUsername(),
                        avatarUrlByUserId.get(post.getUser().getId()),
                        post.getCaption(),
                        post.getCreatedAt(),
                        toMediaResponses(
                                mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                                originalByMediaId,
                                thumbnailByMediaId
                        ),
                        tagsByPostId.getOrDefault(post.getId(), List.of()),
                        commentCountByPostId.getOrDefault(post.getId(), 0L),
                        likeCountByPostId.getOrDefault(post.getId(), 0L),
                        likedPostIds.contains(post.getId()),
                        Boolean.TRUE.equals(post.getIsPending()),
                        post.getFlaggedMatchedPostId(),
                        post.getFlaggedMatchedAuthorDisplayName()
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
            Map<UUID, MediaVariant> originalByMediaId,
            Map<UUID, MediaVariant> thumbnailByMediaId
    ) {
        return mediaList.stream()
                .map(media -> {
                    MediaVariant original = originalByMediaId.get(media.getId());
                    MediaVariant thumbnail = thumbnailByMediaId.get(media.getId());
                    return new MediaItemResponse(
                            media.getId(),
                            media.getOrderIndex(),
                            original != null ? original.getPublicId() : null,
                            original != null ? original.getUrl() : null,
                            thumbnail != null ? thumbnail.getPublicId() : null,
                            thumbnail != null ? thumbnail.getUrl() : null
                    );
                })
                .toList();
    }

        private Map<UUID, String> loadAvatarUrls(List<UUID> userIds) {
                if (userIds == null || userIds.isEmpty()) {
                        return Map.of();
                }

                Map<UUID, String> avatarUrlByUserId = new HashMap<>();
                for (Profile profile : profileRepository.findByUserIdIn(userIds)) {
                        if (profile.getUser() == null) {
                                continue;
                        }
                        avatarUrlByUserId.put(profile.getUser().getId(), profile.getAvatarUrl());
                }
                return avatarUrlByUserId;
        }
}

package com.example.canvasia.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostLikeResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.ReplaceMediaRequest;
import com.example.canvasia.dto.post.ThumbnailCropRequest;
import com.example.canvasia.dto.post.UpdatePostRequest;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.PostLike;
import com.example.canvasia.entity.PostTag;
import com.example.canvasia.entity.Tag;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.impl.post.PostQueryService;
import com.example.canvasia.service.impl.post.PostTagResolver;
import com.example.canvasia.service.interfaces.PostMediaStorageService;
import com.example.canvasia.service.interfaces.PostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Pattern CAPTION_TAG_PATTERN = Pattern.compile("(^|\\s)([#@][a-z0-9._-]{1,50})", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMediaManager postMediaManager;
    private final PostTagRepository postTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostTagResolver postTagResolver;
    private final PostQueryService postQueryService;

    @Override
    @Transactional
    public PostResponse createPost(String username, CreatePostRequest request, List<MultipartFile> files) {
        User user = getUserByUsername(username);
        List<MultipartFile> safeFiles = normalizeFiles(files);
        if (safeFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required to create a post");
        }

        CreatePostRequest safeRequest = request == null
            ? new CreatePostRequest(null, List.of(), List.of())
                : request;
        Map<Integer, PostMediaStorageService.CropArea> cropAreasByIndex = resolveCropAreasByIndex(
            safeRequest.thumbnailCrops(),
            safeFiles.size()
        );

        Post post = postRepository.save(Post.create(user, normalizeBlank(safeRequest.caption())));

        for (int index = 0; index < safeFiles.size(); index++) {
            MultipartFile file = safeFiles.get(index);
            postMediaManager.createMedia(
                    post,
                    file,
                    index,
                    cropAreasByIndex.get(index)
            );
        }

        replacePostTags(post, mergeTagsFromRequestAndCaption(safeRequest.tags(), safeRequest.caption()));

        return buildPostResponse(post, username);
    }

    @Override
    @Transactional
    public PostResponse updatePost(String username, UUID postId, UpdatePostRequest request, List<MultipartFile> files) {
        Post post = getOwnedActivePost(username, postId);
        List<MultipartFile> safeFiles = normalizeFiles(files);
        UpdatePostRequest safeRequest = request == null
                ? new UpdatePostRequest(null, null, List.of(), List.of(), List.of())
                : request;

        Map<Integer, PostMediaStorageService.CropArea> cropAreasByFileIndex = resolveCropAreasByIndex(
                safeRequest.thumbnailCrops(),
                safeFiles.size()
        );

        List<Media> existingMedia = postMediaManager.findByPostIdOrdered(postId);
        if (existingMedia.isEmpty()) {
            throw new IllegalStateException("Post has no media to update");
        }
        Map<UUID, Media> mediaById = new HashMap<>();
        for (Media media : existingMedia) {
            mediaById.put(media.getId(), media);
        }

        Set<UUID> deleteMediaIds = new HashSet<>(safeList(safeRequest.deleteMediaIds()));
        validateDeleteMediaIds(deleteMediaIds, mediaById);

        List<ReplaceMediaRequest> replaceRequests = safeList(safeRequest.replaceMedia());
        validateReplaceRequests(replaceRequests, mediaById, deleteMediaIds, safeFiles.size());

        Set<Integer> usedFileIndexes = new HashSet<>();
        for (ReplaceMediaRequest replaceRequest : replaceRequests) {
            usedFileIndexes.add(replaceRequest.fileIndex());
        }
        int appendCount = safeFiles.size() - usedFileIndexes.size();
        int expectedMediaCount = existingMedia.size() - deleteMediaIds.size() + appendCount;
        if (expectedMediaCount <= 0) {
            throw new IllegalArgumentException("A post must contain at least one image");
        }

        if (safeRequest.caption() != null) {
            post.updateCaption(normalizeBlank(safeRequest.caption()));
        }
        if (safeRequest.tags() != null) {
            replacePostTags(post, mergeTagsFromRequestAndCaption(safeRequest.tags(), safeRequest.caption()));
        }

        List<Media> mediaToDelete = existingMedia.stream()
            .filter(media -> deleteMediaIds.contains(media.getId()) || hasReplacementTarget(replaceRequests, media.getId()))
                .toList();
        postMediaManager.deleteMediaAndAssets(mediaToDelete);

        for (ReplaceMediaRequest replaceRequest : replaceRequests) {
            Media target = mediaById.get(replaceRequest.mediaId());
            MultipartFile replacementFile = safeFiles.get(replaceRequest.fileIndex());
            postMediaManager.createMedia(
                    post,
                    replacementFile,
                    target.getOrderIndex(),
                    cropAreasByFileIndex.get(replaceRequest.fileIndex())
            );
        }

        int nextOrder = postMediaManager.nextOrderIndex(post.getId());

        for (int fileIndex = 0; fileIndex < safeFiles.size(); fileIndex++) {
            if (usedFileIndexes.contains(fileIndex)) {
                continue;
            }

            postMediaManager.createMedia(
                    post,
                    safeFiles.get(fileIndex),
                    nextOrder,
                    cropAreasByFileIndex.get(fileIndex)
            );
            nextOrder++;
        }

        postMediaManager.normalizeMediaOrder(post.getId());

        return buildPostResponse(post, username);
    }

    @Override
    @Transactional
    public void deletePost(String username, UUID postId) {
        Post post = getOwnedActivePost(username, postId);

        post.moveToTrash();
    }

    @Override
    @Transactional
    public void hardDeletePost(String username, UUID postId) {
        Post post = getOwnedTrashedPost(username, postId);
        hardDeletePostInternal(post);
    }

    @Transactional
    public int hardDeleteExpiredSoftDeletedPosts(LocalDateTime deletedBefore) {
        List<Post> postsToPurge = postRepository.findByIsDeletedTrueAndDeletedAtBefore(deletedBefore);
        for (Post post : postsToPurge) {
            hardDeletePostInternal(post);
        }
        return postsToPurge.size();
    }

    private void hardDeletePostInternal(Post post) {
        List<Media> media = postMediaManager.findByPostIdOrdered(post.getId());
        postMediaManager.deleteMediaAndAssets(media);

        List<PostTag> postTags = postTagRepository.findByPostId(post.getId());
        if (!postTags.isEmpty()) {
            postTagRepository.deleteAll(postTags);
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getPostsByUser(String viewerUsername, String username, int page, int size) {
        return postQueryService.getPostsByUser(viewerUsername, username, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getArchivedPostsByOwner(String username, int page, int size) {
        return postQueryService.getArchivedPostsByOwner(username, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getPostsByTag(String viewerUsername, String tag, int page, int size) {
        return postQueryService.getPostsByTag(viewerUsername, tag, page, size);
    }

    @Override
    @Transactional
    public PostLikeResponse likePost(String username, UUID postId) {
        User user = getUserByUsername(username);
        Post post = getActivePost(postId);

        if (!postLikeRepository.existsByUserUsernameAndPostId(username, postId)) {
            postLikeRepository.save(PostLike.create(user, post));
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        return new PostLikeResponse(postId, likeCount, true);
    }

    @Override
    @Transactional
    public PostLikeResponse unlikePost(String username, UUID postId) {
        Post post = getActivePost(postId);

        postLikeRepository.findByUserUsernameAndPostId(username, postId)
                .ifPresent(postLikeRepository::delete);

        long likeCount = postLikeRepository.countByPostId(postId);
        return new PostLikeResponse(post.getId(), likeCount, false);
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Post getOwnedActivePost(String username, UUID postId) {
        Post post = postRepository.findByIdAndUserUsername(postId, username)
                .orElseThrow(() -> new IllegalArgumentException("Post not found or access denied"));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post has already been deleted");
        }
        return post;
    }

    private Post getActivePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post has already been deleted");
        }
        return post;
    }

    private Post getOwnedTrashedPost(String username, UUID postId) {
        return postRepository.findByIdAndUserUsernameAndIsDeletedTrue(postId, username)
                .orElseThrow(() -> new IllegalArgumentException("Archived post not found or access denied"));
    }

    private Map<Integer, PostMediaStorageService.CropArea> resolveCropAreasByIndex(
            List<ThumbnailCropRequest> thumbnailCrops,
            int mediaCount
    ) {
        if (thumbnailCrops == null || thumbnailCrops.isEmpty()) {
            return Map.of();
        }

        Map<Integer, PostMediaStorageService.CropArea> cropAreas = new HashMap<>();
        for (ThumbnailCropRequest crop : thumbnailCrops) {
            int index = crop.index();
            if (index < 0 || index >= mediaCount) {
                throw new IllegalArgumentException("thumbnailCrops[].index is out of range for uploaded media");
            }
            if (cropAreas.containsKey(index)) {
                throw new IllegalArgumentException("thumbnailCrops[] contains duplicate index: " + index);
            }

            cropAreas.put(index, new PostMediaStorageService.CropArea(
                    crop.x(),
                    crop.y(),
                    crop.width(),
                    crop.height()
            ));
        }

        return cropAreas;
    }

    private void validateDeleteMediaIds(Set<UUID> deleteMediaIds, Map<UUID, Media> mediaById) {
        for (UUID mediaId : deleteMediaIds) {
            if (!mediaById.containsKey(mediaId)) {
                throw new IllegalArgumentException("deleteMediaIds contains media that does not belong to this post");
            }
        }
    }

    private void validateReplaceRequests(
            List<ReplaceMediaRequest> replaceRequests,
            Map<UUID, Media> mediaById,
            Set<UUID> deleteMediaIds,
            int uploadedFileCount
    ) {
        Set<UUID> seenMediaIds = new HashSet<>();
        Set<Integer> seenFileIndexes = new HashSet<>();

        for (ReplaceMediaRequest replaceRequest : replaceRequests) {
            UUID mediaId = replaceRequest.mediaId();
            int fileIndex = replaceRequest.fileIndex();

            if (!mediaById.containsKey(mediaId)) {
                throw new IllegalArgumentException("replaceMedia contains media that does not belong to this post");
            }
            if (deleteMediaIds.contains(mediaId)) {
                throw new IllegalArgumentException("A media item cannot be both deleted and replaced");
            }
            if (fileIndex < 0 || fileIndex >= uploadedFileCount) {
                throw new IllegalArgumentException("replaceMedia[].fileIndex is out of range for uploaded media");
            }
            if (!seenMediaIds.add(mediaId)) {
                throw new IllegalArgumentException("replaceMedia contains duplicate mediaId");
            }
            if (!seenFileIndexes.add(fileIndex)) {
                throw new IllegalArgumentException("Each uploaded media file can only be used once");
            }
        }
    }

    private boolean hasReplacementTarget(List<ReplaceMediaRequest> replaceRequests, UUID mediaId) {
        for (ReplaceMediaRequest replaceRequest : replaceRequests) {
            if (replaceRequest.mediaId().equals(mediaId)) {
                return true;
            }
        }
        return false;
    }

    private void replacePostTags(Post post, List<String> rawTags) {
        List<PostTag> existing = postTagRepository.findByPostId(post.getId());
        if (!existing.isEmpty()) {
            postTagRepository.deleteAll(existing);
        }

        List<Tag> resolvedTags = postTagResolver.resolve(rawTags);
        if (resolvedTags.isEmpty()) {
            return;
        }

        List<PostTag> postTags = resolvedTags.stream()
                .map(tag -> PostTag.create(post, tag))
                .toList();
        postTagRepository.saveAll(postTags);
    }

    private PostResponse buildPostResponse(Post post, String viewerUsername) {
        List<MediaItemResponse> mediaResponses = postMediaManager.buildOriginalMediaResponses(post.getId());

        List<String> tagNames = postTagRepository.findByPostId(post.getId())
                .stream()
                .map(postTag -> postTag.getTag().getName())
                .toList();

        long likeCount = postLikeRepository.countByPostId(post.getId());
        boolean likedByMe = viewerUsername != null && postLikeRepository.existsByUserUsernameAndPostId(viewerUsername, post.getId());

        User user = post.getUser();
        return new PostResponse(
                post.getId(),
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                post.getCaption(),
                post.getCreatedAt(),
                mediaResponses,
                tagNames,
                likeCount,
                likedByMe
        );
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> mergeTagsFromRequestAndCaption(List<String> requestTags, String caption) {
        LinkedHashSet<String> mergedTags = new LinkedHashSet<>();

        if (requestTags != null) {
            for (String tag : requestTags) {
                if (tag != null && !tag.isBlank()) {
                    mergedTags.add(tag.trim());
                }
            }
        }

        if (caption != null && !caption.isBlank()) {
            Matcher matcher = CAPTION_TAG_PATTERN.matcher(caption);
            while (matcher.find()) {
                mergedTags.add(matcher.group(2));
            }
        }

        return List.copyOf(mergedTags);
    }
}

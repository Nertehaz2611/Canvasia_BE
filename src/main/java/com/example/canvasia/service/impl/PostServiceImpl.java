package com.example.canvasia.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.dto.post.PostAllowedViewerResponse;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostLikeResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.PostSaveResponse;
import com.example.canvasia.dto.post.ReplaceMediaRequest;
import com.example.canvasia.dto.post.ThumbnailCropRequest;
import com.example.canvasia.dto.post.UpdatePostRequest;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.PostAllowedViewer;
import com.example.canvasia.entity.PostLike;
import com.example.canvasia.entity.PostSave;
import com.example.canvasia.entity.PostTag;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.Tag;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.FollowRepository;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostAllowedViewerRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.PostSaveRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.enums.PostVisibility;
import com.example.canvasia.service.impl.post.PostQueryService;
import com.example.canvasia.service.impl.post.PostTagResolver;
import com.example.canvasia.service.interfaces.NotificationService;
import com.example.canvasia.service.interfaces.PostDeletionService;
import com.example.canvasia.service.interfaces.PostMediaStorageService;
import com.example.canvasia.service.interfaces.PostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    private static final Pattern CAPTION_TAG_PATTERN = Pattern.compile("(^|\\s)([#@][a-z0-9._-]{1,50})", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMediaManager postMediaManager;
    private final PostAllowedViewerRepository postAllowedViewerRepository;
    private final PostTagRepository postTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final PostTagResolver postTagResolver;
    private final PostQueryService postQueryService;
    private final ProfileRepository profileRepository;
    private final ModerationQueuePublisher moderationQueuePublisher;
    private final NotificationService notificationService;
    private final PostDeletionService postDeletionService;

    @Override
    @Transactional
    public PostResponse createPost(String username, CreatePostRequest request, List<MultipartFile> files) {
        User user = getUserByUsername(username);
        List<MultipartFile> safeFiles = normalizeFiles(files);
        if (safeFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required to create a post");
        }

        CreatePostRequest safeRequest = request == null
            ? new CreatePostRequest(null, List.of(), List.of(), PostVisibility.PUBLIC, List.of())
                : request;
        Map<Integer, PostMediaStorageService.CropArea> cropAreasByIndex = resolveCropAreasByIndex(
            safeRequest.thumbnailCrops(),
            safeFiles.size()
        );

        Post post = postRepository.save(Post.create(user, normalizeBlank(safeRequest.caption())));
        applyPostAudience(post, user, safeRequest.visibility(), safeRequest.allowedViewerUserIds());

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

        PostResponse response = buildPostResponse(post, username);
        for (MediaItemResponse media : response.media()) {
            moderationQueuePublisher.publishUpsert(media.mediaId().toString(), media.originalUrl());
        }

        return response;
    }

    @Override
    @Transactional
    public PostResponse updatePost(String username, UUID postId, UpdatePostRequest request, List<MultipartFile> files) {
        Post post = getOwnedActivePost(username, postId);
        List<MultipartFile> safeFiles = normalizeFiles(files);
        UpdatePostRequest safeRequest = request == null
            ? new UpdatePostRequest(null, null, List.of(), List.of(), List.of(), null, List.of())
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
        if (safeRequest.visibility() != null || safeRequest.allowedViewerUserIds() != null) {
            applyPostAudience(post, post.getUser(), safeRequest.visibility(), safeRequest.allowedViewerUserIds());
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

        List<Media> media = postMediaManager.findByPostIdOrdered(postId);
        for (Media item : media) {
            moderationQueuePublisher.publishDelete(item.getId().toString());
        }

        post.moveToTrash();
    }

    @Override
    @Transactional
    public void hardDeletePost(String username, UUID postId) {
        Post post = getOwnedTrashedPost(username, postId);
        postDeletionService.hardDeletePost(post);
    }

    @Transactional
    public int hardDeleteExpiredSoftDeletedPosts(LocalDateTime deletedBefore) {
        List<Post> postsToPurge = postRepository.findByIsDeletedTrueAndDeletedAtBefore(deletedBefore);
        for (Post post : postsToPurge) {
            postDeletionService.hardDeletePost(post);
        }
        return postsToPurge.size();
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
    public PostFeedResponse getPendingPostsByOwner(String username, int page, int size) {
        return postQueryService.getPendingPostsByOwner(username, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getPostsByTag(String viewerUsername, String tag, int page, int size) {
        return postQueryService.getPostsByTag(viewerUsername, tag, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPostFeedResponse searchPosts(String viewerUsername, String query, int limit, String cursor) {
        return postQueryService.getSearchPostsByCursor(limit, cursor, query, viewerUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(String viewerUsername, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post not found");
        }

        boolean isOwner = viewerUsername != null
                && post.getUser() != null
                && viewerUsername.equals(post.getUser().getUsername());

        boolean isAdmin = !isOwner && viewerUsername != null
                && userRepository.findByUsername(viewerUsername)
                        .map(u -> u.getRole() != null && u.getRole().name().equals("ADMIN"))
                        .orElse(false);

        if (Boolean.TRUE.equals(post.getIsPending()) && !isOwner && !isAdmin) {
            throw new IllegalArgumentException("Post not found");
        }

        if (!isOwner && !isAdmin && !canViewerAccessPost(post, viewerUsername)) {
            throw new IllegalArgumentException("Post not found");
        }

        return buildPostResponse(post, viewerUsername);
    }

    @Override
    @Transactional
    public PostLikeResponse likePost(String username, UUID postId) {
        User user = getUserByUsername(username);
        Post post = getActivePostForViewer(username, postId);

        if (!postLikeRepository.existsByUserUsernameAndPostId(username, postId)) {
            postLikeRepository.save(PostLike.create(user, post));
            safeNotify(() -> notificationService.notifyPostLike(post, user), "post like", postId);
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        return new PostLikeResponse(postId, likeCount, true);
    }

    @Override
    @Transactional
    public PostLikeResponse unlikePost(String username, UUID postId) {
        Post post = getActivePostForViewer(username, postId);

        postLikeRepository.findByUserUsernameAndPostId(username, postId)
                .ifPresent(postLikeRepository::delete);

        long likeCount = postLikeRepository.countByPostId(postId);
        return new PostLikeResponse(post.getId(), likeCount, false);
    }

    @Override
    @Transactional
    public PostSaveResponse savePost(String username, UUID postId) {
        User user = getUserByUsername(username);
        Post post = getActivePostForViewer(username, postId);

        if (!postSaveRepository.existsByUserUsernameAndPostId(username, postId)) {
            postSaveRepository.save(PostSave.create(user, post));
        }

        return new PostSaveResponse(postId, true);
    }

    @Override
    @Transactional
    public PostSaveResponse unsavePost(String username, UUID postId) {
        Post post = getActivePostForViewer(username, postId);

        postSaveRepository.findByUserUsernameAndPostId(username, postId)
                .ifPresent(postSaveRepository::delete);

        return new PostSaveResponse(post.getId(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getSavedPosts(String username, int page, int size) {
        return postQueryService.getSavedPosts(username, page, size);
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

    private Post getActivePostForViewer(String viewerUsername, UUID postId) {
        Post post = getActivePost(postId);
        if (!canViewerAccessPost(post, viewerUsername)) {
            throw new IllegalArgumentException("Post not found");
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
        boolean savedByMe = viewerUsername != null && postSaveRepository.existsByUserUsernameAndPostId(viewerUsername, post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());

        User user = post.getUser();
        String avatarUrl = profileRepository.findByUserId(user.getId())
                .map(Profile::getAvatarUrl)
                .orElse(null);
        String displayName = user.getDisplayName();
        String username = user.getUsername();
        String caption = post.getCaption();
        List<PostAllowedViewerResponse> allowedViewers = resolveAllowedViewers(post, viewerUsername);
        return new PostResponse(
                post.getId(),
                user.getId(),
                displayName,
                username,
                avatarUrl,
                caption,
                post.getCreatedAt(),
                mediaResponses,
                tagNames,
                commentCount,
                likeCount,
                likedByMe,
                savedByMe,
                Boolean.TRUE.equals(post.getIsPending()),
                Boolean.TRUE.equals(post.getIsRejected()),
                post.getFlaggedMatchedPostId(),
                post.getFlaggedMatchedAuthorDisplayName(),
                post.getVisibility() == null ? PostVisibility.PUBLIC.name() : post.getVisibility().name(),
                allowedViewers
        );
    }

    private void applyPostAudience(Post post, User owner, PostVisibility requestedVisibility, List<UUID> requestedUserIds) {
        PostVisibility visibility = requestedVisibility == null ? PostVisibility.PUBLIC : requestedVisibility;
        post.updateVisibility(visibility);

        postAllowedViewerRepository.deleteByPostId(post.getId());

        if (visibility != PostVisibility.SELECTED_USERS) {
            return;
        }

        List<UUID> distinctUserIds = new ArrayList<>(new LinkedHashSet<>(safeList(requestedUserIds)));
        if (distinctUserIds.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one follower for selected audience visibility");
        }

        List<User> selectedUsers = userRepository.findAllById(distinctUserIds);
        if (selectedUsers.size() != distinctUserIds.size()) {
            throw new IllegalArgumentException("Some selected users were not found");
        }

        List<UUID> followerIds = followRepository.findFollowerIdsByFollowingIdAndFollowerIdsIn(owner.getId(), distinctUserIds);
        Set<UUID> followerIdSet = new HashSet<>(followerIds);
        for (UUID userId : distinctUserIds) {
            if (!followerIdSet.contains(userId)) {
                throw new IllegalArgumentException("Selected users must be followers of the post owner");
            }
        }

        List<PostAllowedViewer> viewers = selectedUsers.stream()
                .map(selectedUser -> PostAllowedViewer.create(post, selectedUser))
                .toList();
        postAllowedViewerRepository.saveAll(viewers);
    }

    private boolean canViewerAccessPost(Post post, String viewerUsername) {
        if (post == null || Boolean.TRUE.equals(post.getIsDeleted()) || Boolean.TRUE.equals(post.getIsPending())) {
            return false;
        }
        if (viewerUsername != null && post.getUser() != null && viewerUsername.equals(post.getUser().getUsername())) {
            return true;
        }

        PostVisibility visibility = post.getVisibility() == null ? PostVisibility.PUBLIC : post.getVisibility();
        return switch (visibility) {
            case PUBLIC -> true;
            case ONLY_ME -> false;
            case FOLLOWERS -> viewerUsername != null
                    && followRepository.existsByFollowerUsernameAndFollowingUsername(viewerUsername, post.getUser().getUsername());
            case SELECTED_USERS -> viewerUsername != null
                    && postAllowedViewerRepository.existsByPostIdAndUserUsername(post.getId(), viewerUsername);
        };
    }

    private List<PostAllowedViewerResponse> resolveAllowedViewers(Post post, String viewerUsername) {
        if (viewerUsername == null || post.getUser() == null || !viewerUsername.equals(post.getUser().getUsername())) {
            return List.of();
        }

        List<PostAllowedViewer> allowedViewers = postAllowedViewerRepository.findByPostIdWithUser(post.getId());
        if (allowedViewers.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = allowedViewers.stream()
                .map(item -> item.getUser().getId())
                .toList();
        Map<UUID, String> avatarByUserId = new HashMap<>();
        for (Profile profile : profileRepository.findByUserIdIn(userIds)) {
            if (profile.getUser() == null) {
                continue;
            }
            avatarByUserId.put(profile.getUser().getId(), profile.getAvatarUrl());
        }

        return allowedViewers.stream()
                .map(item -> new PostAllowedViewerResponse(
                        item.getUser().getId(),
                        item.getUser().getUsername(),
                        item.getUser().getDisplayName(),
                        avatarByUserId.get(item.getUser().getId())
                ))
                .toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void safeNotify(Runnable action, String actionLabel, UUID postId) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            logger.warn("[Notification] Failed to send {} notification for post {}", actionLabel, postId, ex);
        }
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

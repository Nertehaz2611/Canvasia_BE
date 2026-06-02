package com.example.canvasia.service.impl;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.moderation.ModerationCallbackRequest;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.ModerationService;
import com.example.canvasia.service.interfaces.NotificationService;
import com.example.canvasia.service.interfaces.PostDeletionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private static final Logger logger = LoggerFactory.getLogger(ModerationServiceImpl.class);

    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostDeletionService postDeletionService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void handleCallback(ModerationCallbackRequest request) {
        if (request == null || request.mediaId() == null) {
            return;
        }

        Media media = mediaRepository.findById(request.mediaId()).orElse(null);
        if (media == null) {
            return;
        }

        Media matchedMedia = request.matchedMediaId() != null
                ? mediaRepository.findByIdWithPostAndUser(request.matchedMediaId()).orElse(null)
                : null;

        logCallback(request, media, matchedMedia);

        Post post = media.getPost();
        if (post == null || Boolean.TRUE.equals(post.getIsDeleted())) {
            return;
        }

        applyModerationStatus(request.status(), media.getUserId(), post, matchedMedia);
    }

    @Override
    @Transactional
    public void approvePendingPost(UUID postId, String adminUsername) {
        Post post = getPendingPost(postId);
        User admin = requireUser(adminUsername);

        post.markPending(false);
        postRepository.save(post);
        safeNotify(() -> notificationService.notifyPostApproved(post, admin), "post approved", postId);
    }

    @Override
    @Transactional
    public void deletePendingPost(UUID postId, String adminUsername) {
        Post post = getPendingPost(postId);
        User admin = requireUser(adminUsername);

        safeNotify(() -> notificationService.notifyPostDeleted(post, admin), "post deleted", postId);
        postDeletionService.hardDeletePost(post);
    }

    private void applyModerationStatus(String status, UUID uploaderUserId, Post post, Media matchedMedia) {
        if (status == null || !"PENDING".equals(status.trim().toUpperCase(Locale.ROOT))) {
            return;
        }

        UUID originalAuthorUserId = matchedMedia != null ? matchedMedia.getUserId() : null;
        boolean sameOwner = uploaderUserId != null && uploaderUserId.equals(originalAuthorUserId);

        if (sameOwner) {
            logger.info("[Moderation] Skipped pending for post {} — same owner re-upload (userId={})",
                    post.getId(), uploaderUserId);
            return;
        }

        boolean wasPending = Boolean.TRUE.equals(post.getIsPending());
        post.markPending(true);
        if (matchedMedia != null && matchedMedia.getPost() != null) {
            Post matchedPost = matchedMedia.getPost();
            String authorDisplayName = matchedPost.getUser() != null
                    ? matchedPost.getUser().getDisplayName() : null;
            post.flagWith(matchedPost.getId(), authorDisplayName);
            logger.info("[Moderation] Post {} marked PENDING (original author='{}')", post.getId(), authorDisplayName);
        }
        postRepository.save(post);

        if (!wasPending) {
            notificationService.notifyPostPending(post);
        }
    }

    private void logCallback(ModerationCallbackRequest request, Media media, Media matchedMedia) {
        UUID originalAuthorUserId = matchedMedia != null ? matchedMedia.getUserId() : null;
        logger.info("[Moderation] mediaId={} uploaderUserId={} matchedMediaId={} originalAuthorUserId={} similarity={}",
                request.mediaId(), media.getUserId(),
                request.matchedMediaId(), originalAuthorUserId,
                request.similarity());
    }

    private Post getPendingPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post has already been deleted");
        }
        if (!Boolean.TRUE.equals(post.getIsPending())) {
            throw new IllegalArgumentException("Post is not pending");
        }
        return post;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void safeNotify(Runnable action, String actionLabel, UUID postId) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            logger.warn("[Notification] Failed to send {} notification for post {}", actionLabel, postId, ex);
        }
    }
}
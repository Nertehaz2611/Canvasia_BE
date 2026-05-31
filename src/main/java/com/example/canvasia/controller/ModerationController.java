package com.example.canvasia.controller;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.moderation.ModerationCallbackRequest;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Post;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private static final Logger logger = LoggerFactory.getLogger(ModerationController.class);

    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody ModerationCallbackRequest request) {
        if (request == null || request.mediaId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Media media = mediaRepository.findById(request.mediaId()).orElse(null);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        Media matchedMedia = request.matchedMediaId() != null
                ? mediaRepository.findByIdWithPostAndUser(request.matchedMediaId()).orElse(null)
                : null;

        logCallback(request, media, matchedMedia);

        Post post = media.getPost();
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            return ResponseEntity.ok().build();
        }

        applyModerationStatus(request.status(), media.getUserId(), post, matchedMedia);

        return ResponseEntity.ok().build();
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

        post.markPending(true);
        if (matchedMedia != null && matchedMedia.getPost() != null) {
            Post matchedPost = matchedMedia.getPost();
            String authorDisplayName = matchedPost.getUser() != null
                    ? matchedPost.getUser().getDisplayName() : null;
            post.flagWith(matchedPost.getId(), authorDisplayName);
            logger.info("[Moderation] Post {} marked PENDING (original author='{}')", post.getId(), authorDisplayName);
        }
        postRepository.save(post);
    }

    private void logCallback(ModerationCallbackRequest request, Media media, Media matchedMedia) {
        UUID originalAuthorUserId = matchedMedia != null ? matchedMedia.getUserId() : null;
        logger.info("[Moderation] mediaId={} uploaderUserId={} matchedMediaId={} originalAuthorUserId={} similarity={}",
                request.mediaId(), media.getUserId(),
                request.matchedMediaId(), originalAuthorUserId,
                request.similarity());
    }
}

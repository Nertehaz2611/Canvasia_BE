package com.example.canvasia.controller;

import java.util.Locale;

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

    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody ModerationCallbackRequest request) {
        if (request == null || request.mediaId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Media media = mediaRepository.findById(request.mediaId())
                .orElse(null);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        Post post = media.getPost();
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            return ResponseEntity.ok().build();
        }

        String status = request.status();
        if (status != null) {
            boolean pending = "PENDING".equals(status.trim().toUpperCase(Locale.ROOT));
            post.markPending(pending);
            postRepository.save(post);
        }

        return ResponseEntity.ok().build();
    }
}

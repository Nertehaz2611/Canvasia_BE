package com.example.canvasia.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.canvasia.dto.comment.CommentFeedResponse;
import com.example.canvasia.dto.comment.CommentLikeResponse;
import com.example.canvasia.dto.comment.CommentResponse;
import com.example.canvasia.dto.comment.CreateCommentRequest;
import com.example.canvasia.service.interfaces.CommentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    public CommentResponse createComment(
            Authentication authentication,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return commentService.createComment(authentication.getName(), postId, request);
    }

    @PutMapping("/{commentId}")
    @SecurityRequirement(name = "bearerAuth")
    public CommentResponse updateComment(
            Authentication authentication,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return commentService.updateComment(authentication.getName(), commentId, request);
    }

    @PostMapping("/{commentId}/replies")
    @SecurityRequirement(name = "bearerAuth")
    public CommentResponse replyComment(
            Authentication authentication,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return commentService.replyComment(authentication.getName(), commentId, request);
    }

    @GetMapping("/{postId}")
    public CommentFeedResponse getPostComments(
            Authentication authentication,
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "2") int maxDepth
    ) {
        return commentService.getCommentsByPost(extractViewerUsername(authentication), postId, page, size, maxDepth);
    }

    @PostMapping("/{commentId}/likes")
    @SecurityRequirement(name = "bearerAuth")
    public CommentLikeResponse likeComment(
            Authentication authentication,
            @PathVariable UUID commentId
    ) {
        return commentService.likeComment(authentication.getName(), commentId);
    }

    @DeleteMapping("/{commentId}/likes")
    @SecurityRequirement(name = "bearerAuth")
    public CommentLikeResponse unlikeComment(
            Authentication authentication,
            @PathVariable UUID commentId
    ) {
        return commentService.unlikeComment(authentication.getName(), commentId);
    }

    @DeleteMapping("/{commentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteComment(
            Authentication authentication,
            @PathVariable UUID commentId
    ) {
        commentService.deleteComment(authentication.getName(), commentId);
        return ResponseEntity.noContent().build();
    }

    private String extractViewerUsername(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}

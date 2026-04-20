package com.example.canvasia.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.controller.support.PostRequestResolver;
import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.PostLikeResponse;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.UpdatePostRequest;
import com.example.canvasia.service.interfaces.PostService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;
    private final PostRequestResolver postRequestResolver;

    @PostMapping(consumes = {"multipart/form-data"})
    @SecurityRequirement(name = "bearerAuth")
    public PostResponse createPost(
            Authentication authentication,
            @RequestPart(value = "payload", required = false) String payload,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestPart("media") List<MultipartFile> media
    ) {
        CreatePostRequest request = postRequestResolver.resolveCreateRequest(payload, caption, tags);
        return postService.createPost(authentication.getName(), request, media);
    }

    @PutMapping(value = "/{postId}", consumes = {"multipart/form-data"})
    @SecurityRequirement(name = "bearerAuth")
    public PostResponse updatePost(
            Authentication authentication,
            @PathVariable UUID postId,
            @RequestPart(value = "payload", required = false) String payload,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestPart(value = "media", required = false) List<MultipartFile> media
    ) {
        UpdatePostRequest request = postRequestResolver.resolveUpdateRequest(payload, caption, tags);
        return postService.updatePost(authentication.getName(), postId, request, media);
    }

    @DeleteMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        postService.deletePost(authentication.getName(), postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}/hard")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> hardDeletePost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        postService.hardDeletePost(authentication.getName(), postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/likes")
    @SecurityRequirement(name = "bearerAuth")
    public PostLikeResponse likePost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        return postService.likePost(authentication.getName(), postId);
    }

    @DeleteMapping("/{postId}/likes")
    @SecurityRequirement(name = "bearerAuth")
    public PostLikeResponse unlikePost(
            Authentication authentication,
            @PathVariable UUID postId
    ) {
        return postService.unlikePost(authentication.getName(), postId);
    }

    @GetMapping("/users/{username}")
    public PostFeedResponse getPostsByUser(
            Authentication authentication,
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.getPostsByUser(extractViewerUsername(authentication), username, page, size);
    }

    @GetMapping("/archive")
    @SecurityRequirement(name = "bearerAuth")
    public PostFeedResponse getArchivedPostsByOwner(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.getArchivedPostsByOwner(authentication.getName(), page, size);
    }

    @GetMapping("/tags/{tag}")
    public PostFeedResponse getPostsByTag(
            Authentication authentication,
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.getPostsByTag(extractViewerUsername(authentication), tag, page, size);
    }

    private String extractViewerUsername(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

}

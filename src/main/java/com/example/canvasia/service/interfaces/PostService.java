package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostLikeResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.UpdatePostRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponse createPost(String username, CreatePostRequest request, List<MultipartFile> files);

    PostResponse updatePost(String username, UUID postId, UpdatePostRequest request, List<MultipartFile> files);

    void deletePost(String username, UUID postId);

    PostFeedResponse getPostsByUser(String viewerUsername, String username, int page, int size);

    PostFeedResponse getArchivedPostsByOwner(String username, int page, int size);

    PostFeedResponse getPostsByTag(String viewerUsername, String tag, int page, int size);

    void hardDeletePost(String username, UUID postId);

    PostLikeResponse likePost(String username, UUID postId);

    PostLikeResponse unlikePost(String username, UUID postId);
}

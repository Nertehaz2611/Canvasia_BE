package com.example.canvasia.service.interfaces;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.canvasia.dto.post.CreatePostRequest;
import com.example.canvasia.dto.post.CursorPostFeedResponse;
import com.example.canvasia.dto.post.PostFeedResponse;
import com.example.canvasia.dto.post.PostLikeResponse;
import com.example.canvasia.dto.post.PostSaveResponse;
import com.example.canvasia.dto.post.PostResponse;
import com.example.canvasia.dto.post.UpdatePostRequest;

public interface PostService {

    PostResponse createPost(String username, CreatePostRequest request, List<MultipartFile> files);

    PostResponse updatePost(String username, UUID postId, UpdatePostRequest request, List<MultipartFile> files);

    void deletePost(String username, UUID postId);

    PostFeedResponse getPostsByUser(String viewerUsername, String username, int page, int size);

    PostFeedResponse getArchivedPostsByOwner(String username, int page, int size);

    PostFeedResponse getPendingPostsByOwner(String username, int page, int size);

    PostFeedResponse getPostsByTag(String viewerUsername, String tag, int page, int size);

    CursorPostFeedResponse searchPosts(String viewerUsername, String query, int limit, String cursor);

    PostResponse getPostById(String viewerUsername, UUID postId);

    void hardDeletePost(String username, UUID postId);

    PostLikeResponse likePost(String username, UUID postId);

    PostLikeResponse unlikePost(String username, UUID postId);

    PostSaveResponse savePost(String username, UUID postId);

    PostSaveResponse unsavePost(String username, UUID postId);

    PostFeedResponse getSavedPosts(String username, int page, int size);
}

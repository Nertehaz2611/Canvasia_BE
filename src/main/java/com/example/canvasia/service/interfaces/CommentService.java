package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.comment.CommentFeedResponse;
import com.example.canvasia.dto.comment.CommentLikeResponse;
import com.example.canvasia.dto.comment.CommentResponse;
import com.example.canvasia.dto.comment.CreateCommentRequest;

import java.util.UUID;

public interface CommentService {

    CommentResponse createComment(String username, UUID postId, CreateCommentRequest request);

    CommentResponse updateComment(String username, UUID commentId, CreateCommentRequest request);

    CommentResponse replyComment(String username, UUID parentCommentId, CreateCommentRequest request);

    CommentLikeResponse likeComment(String username, UUID commentId);

    CommentLikeResponse unlikeComment(String username, UUID commentId);

    void deleteComment(String username, UUID commentId);

    CommentFeedResponse getCommentsByPost(String viewerUsername, UUID postId, int page, int size, int maxDepth);
}

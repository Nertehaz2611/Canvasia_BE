package com.example.canvasia.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.PostTag;
import com.example.canvasia.repository.CommentLikeRepository;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.PostReportRepository;
import com.example.canvasia.repository.PostSaveRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.service.interfaces.PostDeletionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostDeletionServiceImpl implements PostDeletionService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final PostReportRepository postReportRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostMediaManager postMediaManager;

    @Override
    @Transactional
    public void hardDeletePost(Post post) {
        if (post == null || post.getId() == null) {
            return;
        }

        UUID postId = post.getId();
        List<Media> media = postMediaManager.findByPostIdOrdered(postId);
        postMediaManager.deleteMediaAndAssets(media);

        List<UUID> commentIds = commentRepository.findIdsByPostId(postId);
        if (!commentIds.isEmpty()) {
            commentLikeRepository.deleteByCommentIdIn(commentIds);
            commentRepository.deleteByPostId(postId);
        }

        postLikeRepository.deleteByPostId(postId);
        postSaveRepository.deleteByPostId(postId);
        postReportRepository.deleteByPostId(postId);

        List<PostTag> postTags = postTagRepository.findByPostId(postId);
        if (!postTags.isEmpty()) {
            postTagRepository.deleteAll(postTags);
        }

        postRepository.delete(post);
    }
}
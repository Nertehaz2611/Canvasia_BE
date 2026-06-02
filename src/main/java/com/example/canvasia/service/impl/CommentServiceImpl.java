package com.example.canvasia.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.comment.CommentFeedResponse;
import com.example.canvasia.dto.comment.CommentLikeResponse;
import com.example.canvasia.dto.comment.CommentResponse;
import com.example.canvasia.dto.comment.CreateCommentRequest;
import com.example.canvasia.entity.Comment;
import com.example.canvasia.entity.CommentLike;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.CommentLikeRepository;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import static com.example.canvasia.service.impl.support.PagingUtils.clampPageSize;
import com.example.canvasia.service.interfaces.NotificationService;
import com.example.canvasia.service.interfaces.CommentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

    private static final int MAX_COMMENT_PAGE_SIZE = 20;
    private static final int FIXED_UI_MAX_DEPTH = 2;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentResponse createComment(String username, UUID postId, CreateCommentRequest request) {
        User user = getUserByUsername(username);
        Post post = getActivePost(postId);
        String content = extractCommentContent(request);

        Comment saved = commentRepository.save(Comment.createRootComment(post, user, content));
        safeNotify(() -> notificationService.notifyPostComment(post, user), "post comment", post.getId());
        return buildSingleCommentResponse(saved, username);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(String username, UUID commentId, CreateCommentRequest request) {
        Comment comment = getCommentById(commentId);
        ensureCommentPostActive(comment);
        if (!comment.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Comment not found or access denied");
        }

        comment.updateContent(extractCommentContent(request));
        return buildSingleCommentResponse(comment, username);
    }

    @Override
    @Transactional
    public CommentResponse replyComment(String username, UUID parentCommentId, CreateCommentRequest request) {
        User user = getUserByUsername(username);
        Comment requestedParent = getCommentById(parentCommentId);
        ensureCommentPostActive(requestedParent);
        String content = extractCommentContent(request);

        // Keep tree at 2 levels for UI: replies to replies are flattened under the root comment.
        boolean isReplyToReply = requestedParent.getParent() != null;
        Comment effectiveParent = isReplyToReply
            ? getRootComment(requestedParent)
            : requestedParent;
        String effectiveContent = isReplyToReply
            ? withMentionPrefix(content, requestedParent.getUser().getUsername())
            : content;

        Comment saved = commentRepository.save(Comment.createReplyComment(effectiveParent, user, effectiveContent));
        safeNotify(() -> notificationService.notifyRootCommentReply(effectiveParent, user), "comment reply", requestedParent.getId());
        return buildSingleCommentResponse(saved, username);
    }

    @Override
    @Transactional
    public CommentLikeResponse likeComment(String username, UUID commentId) {
        User user = getUserByUsername(username);
        Comment comment = getCommentById(commentId);
        ensureCommentPostActive(comment);

        boolean hasLiked = commentLikeRepository.existsByUserUsernameAndCommentId(username, commentId);
        if (!hasLiked) {
            commentLikeRepository.save(CommentLike.create(user, comment));
            safeNotify(() -> notificationService.notifyCommentLike(comment, user), "comment like", commentId);
        }

        long likeCount = commentLikeRepository.countByCommentId(commentId);
        return new CommentLikeResponse(commentId, likeCount, true);
    }

    @Override
    @Transactional
    public CommentLikeResponse unlikeComment(String username, UUID commentId) {
        Comment comment = getCommentById(commentId);
        ensureCommentPostActive(comment);

        commentLikeRepository.findByUserUsernameAndCommentId(username, commentId)
                .ifPresent(commentLikeRepository::delete);

        long likeCount = commentLikeRepository.countByCommentId(commentId);
        return new CommentLikeResponse(comment.getId(), likeCount, false);
    }

    @Override
    @Transactional
    public void deleteComment(String username, UUID commentId) {
        Comment comment = getCommentById(commentId);
        ensureCommentPostActive(comment);

        boolean isCommentOwner = comment.getUser().getUsername().equals(username);
        boolean isPostOwner = comment.getPost() != null
                && comment.getPost().getUser() != null
                && comment.getPost().getUser().getUsername().equals(username);

        if (!isCommentOwner && !isPostOwner) {
            throw new IllegalArgumentException("Comment not found or access denied");
        }

        if (comment.getParent() == null) {
            UUID rootId = comment.getId();
            List<UUID> replyIds = commentRepository.findIdsByRootId(rootId);

            if (!replyIds.isEmpty()) {
                commentLikeRepository.deleteByCommentIdIn(replyIds);
                commentRepository.deleteByRootId(rootId);
            }

            commentLikeRepository.deleteByCommentId(rootId);
            commentRepository.delete(comment);
            return;
        }

        commentLikeRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentFeedResponse getCommentsByPost(String viewerUsername, UUID postId, int page, int size, int maxDepth) {
        getActivePost(postId);

        int safePage = Math.max(page, 0);
        int safeSize = clampPageSize(size, MAX_COMMENT_PAGE_SIZE);
        int safeMaxDepth = sanitizeMaxDepth(maxDepth);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<Comment> roots = commentRepository.findRootCommentsByPostId(postId, pageable);
        if (roots.isEmpty()) {
            return new CommentFeedResponse(List.of(), safePage, safeSize, false, safeMaxDepth);
        }

        List<Comment> rootComments = roots.getContent();
        List<UUID> rootIds = rootComments.stream().map(Comment::getId).toList();

        List<Comment> descendants = commentRepository.findThreadCommentsByPostIdAndRootIds(postId, rootIds);

        Map<UUID, List<Comment>> childrenByParent = indexChildrenByParent(descendants);
        Map<UUID, Long> replyCountByParent = getReplyCountByParent(collectCommentIds(rootComments, descendants));
        Map<UUID, Long> likeCountByComment = getLikeCountByComment(collectCommentIds(rootComments, descendants));
        Set<UUID> likedCommentIds = getLikedCommentIds(viewerUsername, likeCountByComment.keySet());
        Map<UUID, String> avatarUrlByUserId = loadAvatarUrls(collectUserIds(rootComments, descendants));
        CommentRenderContext context = new CommentRenderContext(
            safeMaxDepth,
            childrenByParent,
            replyCountByParent,
            likeCountByComment,
            likedCommentIds,
            avatarUrlByUserId
        );

        List<CommentResponse> items = rootComments.stream()
                .map(root -> toCommentResponse(
                        root,
                        1,
                    context
                ))
                .toList();

        return new CommentFeedResponse(items, safePage, safeSize, roots.hasNext(), safeMaxDepth);
    }

    private CommentResponse buildSingleCommentResponse(Comment comment, String viewerUsername) {
        long likeCount = commentLikeRepository.countByCommentId(comment.getId());
        boolean likedByMe = viewerUsername != null
                && commentLikeRepository.existsByUserUsernameAndCommentId(viewerUsername, comment.getId());
        long replyCount = commentRepository.countRepliesByParentIds(List.of(comment.getId()))
                .stream()
                .findFirst()
                .map(CommentRepository.ReplyCountView::getReplyCount)
                .orElse(0L);

        String avatarUrl = profileRepository.findByUserId(comment.getUser().getId())
            .map(Profile::getAvatarUrl)
            .orElse(null);

        return toLeafCommentResponse(comment, likeCount, likedByMe, replyCount, avatarUrl);
    }

    private CommentResponse toCommentResponse(
            Comment comment,
            int depth,
            CommentRenderContext context
    ) {
        long likeCount = context.likeCountByComment.getOrDefault(comment.getId(), 0L);
        boolean likedByMe = context.likedCommentIds.contains(comment.getId());
        long replyCount = context.replyCountByParent.getOrDefault(comment.getId(), 0L);

        List<CommentResponse> replies;
        if (depth >= context.maxDepth) {
            replies = List.of();
        } else {
            replies = context.childrenByParent.getOrDefault(comment.getId(), List.of())
                    .stream()
                    .map(child -> toCommentResponse(
                            child,
                            depth + 1,
                            context
                    ))
                    .toList();
        }

        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getRootId(),
                comment.getUser().getId(),
                comment.getUser().getDisplayName(),
                comment.getUser().getUsername(),
                context.avatarUrlByUserId.get(comment.getUser().getId()),
                comment.getContent(),
                comment.getCreatedAt(),
                likeCount,
                likedByMe,
                replyCount,
                replies
        );
    }

    private static final class CommentRenderContext {
        private final int maxDepth;
        private final Map<UUID, List<Comment>> childrenByParent;
        private final Map<UUID, Long> replyCountByParent;
        private final Map<UUID, Long> likeCountByComment;
        private final Set<UUID> likedCommentIds;
        private final Map<UUID, String> avatarUrlByUserId;

        private CommentRenderContext(
                int maxDepth,
                Map<UUID, List<Comment>> childrenByParent,
                Map<UUID, Long> replyCountByParent,
                Map<UUID, Long> likeCountByComment,
                Set<UUID> likedCommentIds,
                Map<UUID, String> avatarUrlByUserId
        ) {
            this.maxDepth = maxDepth;
            this.childrenByParent = childrenByParent;
            this.replyCountByParent = replyCountByParent;
            this.likeCountByComment = likeCountByComment;
            this.likedCommentIds = likedCommentIds;
            this.avatarUrlByUserId = avatarUrlByUserId;
        }
    }

    private CommentResponse toLeafCommentResponse(
            Comment comment,
            long likeCount,
            boolean likedByMe,
            long replyCount,
            String avatarUrl
    ) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getRootId(),
                comment.getUser().getId(),
                comment.getUser().getDisplayName(),
                comment.getUser().getUsername(),
                avatarUrl,
                comment.getContent(),
                comment.getCreatedAt(),
                likeCount,
                likedByMe,
                replyCount,
                List.of()
        );
    }

    private Map<UUID, String> loadAvatarUrls(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> avatarUrlByUserId = new HashMap<>();
        for (Profile profile : profileRepository.findByUserIdIn(userIds)) {
            if (profile.getUser() == null) {
                continue;
            }
            avatarUrlByUserId.put(profile.getUser().getId(), profile.getAvatarUrl());
        }
        return avatarUrlByUserId;
    }

    private Map<UUID, List<Comment>> indexChildrenByParent(List<Comment> descendants) {
        Map<UUID, List<Comment>> index = new HashMap<>();
        for (Comment comment : descendants) {
            Comment parent = comment.getParent();
            if (parent == null) {
                continue;
            }
            index.computeIfAbsent(parent.getId(), key -> new ArrayList<>()).add(comment);
        }
        return index;
    }

    private Map<UUID, Long> getReplyCountByParent(Collection<UUID> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> counts = new HashMap<>();
        for (CommentRepository.ReplyCountView view : commentRepository.countRepliesByParentIds(parentIds)) {
            counts.put(view.getParentId(), view.getReplyCount());
        }
        return counts;
    }

    private Map<UUID, Long> getLikeCountByComment(Collection<UUID> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> counts = new HashMap<>();
        for (CommentLikeRepository.CommentLikeCountView view : commentLikeRepository.countByCommentIds(commentIds)) {
            counts.put(view.getCommentId(), view.getLikeCount());
        }
        return counts;
    }

    private Set<UUID> getLikedCommentIds(String viewerUsername, Collection<UUID> commentIds) {
        if (viewerUsername == null || viewerUsername.isBlank() || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(commentLikeRepository.findLikedCommentIdsByUsernameAndCommentIds(viewerUsername, commentIds));
    }

    private Set<UUID> collectCommentIds(List<Comment> roots, List<Comment> descendants) {
        Set<UUID> ids = new HashSet<>();
        for (Comment root : roots) {
            ids.add(root.getId());
        }
        for (Comment descendant : descendants) {
            ids.add(descendant.getId());
        }
        return ids;
    }

    private Set<UUID> collectUserIds(List<Comment> roots, List<Comment> descendants) {
        Set<UUID> ids = new HashSet<>();
        for (Comment root : roots) {
            if (root.getUser() != null) {
                ids.add(root.getUser().getId());
            }
        }
        for (Comment descendant : descendants) {
            if (descendant.getUser() != null) {
                ids.add(descendant.getUser().getId());
            }
        }
        return ids;
    }

    private int sanitizeMaxDepth(int maxDepth) {
        if (maxDepth <= 0) {
            return FIXED_UI_MAX_DEPTH;
        }
        return Math.min(maxDepth, FIXED_UI_MAX_DEPTH);
    }

    private Comment getRootComment(Comment comment) {
        UUID rootId = comment.getRootId();
        if (rootId == null) {
            return comment;
        }
        return commentRepository.findByIdAndPostId(rootId, comment.getPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("Root comment not found"));
    }

    private String withMentionPrefix(String content, String username) {
        String trimmed = content == null ? "" : content.trim();
        String mention = "@" + username;
        if (trimmed.startsWith(mention + " ") || trimmed.equals(mention)) {
            return trimmed;
        }
        return mention + " " + trimmed;
    }

    private String extractCommentContent(CreateCommentRequest request) {
        if (request == null || request.content() == null) {
            throw new IllegalArgumentException("Comment payload is required");
        }
        return request.content();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void safeNotify(Runnable action, String actionLabel, UUID referenceId) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            logger.warn("[Notification] Failed to send {} notification for reference {}", actionLabel, referenceId, ex);
        }
    }

    private Post getActivePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post has already been deleted");
        }
        return post;
    }

    private void ensureCommentPostActive(Comment comment) {
        Post post = comment.getPost();
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalArgumentException("Post has already been deleted");
        }
    }

    private Comment getCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    }

}

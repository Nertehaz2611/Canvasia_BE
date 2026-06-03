package com.example.canvasia.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.canvasia.dto.admin.AdminPendingPostFeedResponse;
import com.example.canvasia.dto.admin.AdminPendingPostItem;
import com.example.canvasia.dto.admin.AdminReportItem;
import com.example.canvasia.dto.admin.AdminReportedPostFeedResponse;
import com.example.canvasia.dto.admin.AdminReportedPostItem;
import com.example.canvasia.dto.admin.AdminStatsResponse;
import com.example.canvasia.dto.admin.AdminUserFeedResponse;
import com.example.canvasia.dto.admin.AdminUserItem;
import com.example.canvasia.dto.post.MediaItemResponse;
import com.example.canvasia.entity.Media;
import com.example.canvasia.entity.MediaVariant;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.PostReport;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.MediaVariantType;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.MediaRepository;
import com.example.canvasia.repository.MediaVariantRepository;
import com.example.canvasia.repository.PostLikeRepository;
import com.example.canvasia.repository.PostRepository;
import com.example.canvasia.repository.PostReportRepository;
import com.example.canvasia.repository.PostTagRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.AdminService;
import com.example.canvasia.service.interfaces.NotificationService;
import com.example.canvasia.service.interfaces.PostDeletionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int MAX_PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReportRepository postReportRepository;
    private final PostTagRepository postTagRepository;
    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final ProfileRepository profileRepository;
    private final PostDeletionService postDeletionService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long newUsersLast7Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));
        long totalPosts = postRepository.countByIsDeletedFalseAndIsPendingFalse();
        long totalComments = commentRepository.countActiveComments();
        long totalLikes = postLikeRepository.countActiveLikes();
        long totalReports = postReportRepository.count();

        return new AdminStatsResponse(totalUsers, newUsersLast7Days, totalPosts, totalComments, totalLikes, totalReports);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserFeedResponse getUsers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt")));
        Page<User> usersPage = userRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<UUID> userIds = usersPage.getContent().stream().map(User::getId).toList();
        Map<UUID, String> avatarByUserId = loadAvatarUrlsForUsers(userIds);

        List<AdminUserItem> items = usersPage.getContent().stream()
                .map(user -> new AdminUserItem(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        avatarByUserId.get(user.getId()),
                        user.getRole() != null ? user.getRole().name() : null,
                        user.getStatus() != null ? user.getStatus().name() : null,
                        user.getCreatedAt()
                ))
                .toList();

        return new AdminUserFeedResponse(items, safePage, safeSize, usersPage.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPendingPostFeedResponse getPendingPosts(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postsPage = postRepository.findByIsDeletedFalseAndIsPendingTrueAndIsRejectedFalse(pageable);

        List<AdminPendingPostItem> items = buildPendingPostItems(postsPage.getContent());
        return new AdminPendingPostFeedResponse(items, safePage, safeSize, postsPage.hasNext());
    }

    @Override
    @Transactional
    public void approvePendingPost(UUID postId, String adminUsername) {
        Post post = getPendingPost(postId);
        User admin = requireUser(adminUsername);
        post.markPending(false);
        post.markRejected(false);
        postRepository.save(post);
        notificationService.notifyPostApproved(post, admin);
    }

    @Override
    @Transactional
    public void rejectPendingPost(UUID postId, String adminUsername) {
        Post post = getPendingPost(postId);
        User admin = requireUser(adminUsername);
        post.markPending(false);
        post.markRejected(true);
        postRepository.save(post);
        notificationService.notifyPostRejected(post, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportedPostFeedResponse getReportedPosts(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<UUID> reportedPostIds = postReportRepository.findDistinctReportedPostIds(pageable);
        if (reportedPostIds.isEmpty()) {
            return new AdminReportedPostFeedResponse(List.of(), safePage, safeSize, false);
        }

        List<UUID> postIds = reportedPostIds.getContent();
        List<Post> posts = postRepository.findAllById(postIds);
        Map<UUID, Post> postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));

        List<PostReport> allReports = postReportRepository.findByPostIdIn(postIds);
        Map<UUID, List<PostReport>> reportsByPostId = allReports.stream()
                .collect(Collectors.groupingBy(r -> r.getPost().getId()));

        List<UUID> userIds = posts.stream().map(p -> p.getUser().getId()).distinct().toList();
        Map<UUID, String> avatarByUserId = loadAvatarUrlsForUsers(userIds);

        List<UUID> allMediaPostIds = postIds;
        List<Media> mediaList = mediaRepository.findByPostIdInOrderByOrderIndexAsc(allMediaPostIds);
        Map<UUID, List<Media>> mediaByPostId = mediaList.stream()
                .collect(Collectors.groupingBy(m -> m.getPost().getId(), LinkedHashMap::new, Collectors.toList()));

        List<UUID> mediaIds = mediaList.stream().map(Media::getId).toList();
        Map<UUID, MediaVariant> originalByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.ORIGINAL)
                .stream()
                .collect(Collectors.toMap(v -> v.getMedia().getId(), v -> v));
        Map<UUID, MediaVariant> thumbnailByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.THUMBNAIL)
                .stream()
                .collect(Collectors.toMap(v -> v.getMedia().getId(), v -> v));

        Map<UUID, List<String>> tagsByPostId = postTagRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getPost().getId(),
                        Collectors.mapping(pt -> pt.getTag().getName(), Collectors.toList())
                ));

        Map<UUID, Long> likeCountByPostId = postLikeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostLikeRepository.PostLikeCountView::getPostId,
                        PostLikeRepository.PostLikeCountView::getLikeCount));

        Map<UUID, Long> commentCountByPostId = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(CommentRepository.PostCommentCountView::getPostId,
                        CommentRepository.PostCommentCountView::getCommentCount));

        List<AdminReportedPostItem> items = postIds.stream()
                .map(postsById::get)
                .filter(p -> p != null)
                .map(post -> {
                    List<PostReport> reports = reportsByPostId.getOrDefault(post.getId(), List.of());
                    List<AdminReportItem> reportItems = reports.stream()
                            .map(r -> {
                                User reporter = r.getReporter();
                                String reporterAvatarUrl = reporter != null
                                        ? avatarByUserId.get(reporter.getId()) : null;
                                return new AdminReportItem(
                                        r.getId(),
                                        reporter != null ? reporter.getId() : null,
                                        reporter != null ? reporter.getUsername() : null,
                                        reporter != null ? reporter.getDisplayName() : null,
                                        reporterAvatarUrl,
                                        r.getReasons().stream().map(Enum::name).toList(),
                                        r.getOtherReason(),
                                        r.getCreatedAt()
                                );
                            })
                            .toList();

                    return new AdminReportedPostItem(
                            post.getId(),
                            post.getUser().getId(),
                            post.getUser().getDisplayName(),
                            post.getUser().getUsername(),
                            avatarByUserId.get(post.getUser().getId()),
                            post.getCaption(),
                            post.getCreatedAt(),
                            toMediaResponses(
                                    mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                                    originalByMediaId,
                                    thumbnailByMediaId
                            ),
                            tagsByPostId.getOrDefault(post.getId(), List.of()),
                            commentCountByPostId.getOrDefault(post.getId(), 0L),
                            likeCountByPostId.getOrDefault(post.getId(), 0L),
                            reports.size(),
                            reportItems
                    );
                })
                .toList();

        return new AdminReportedPostFeedResponse(items, safePage, safeSize, reportedPostIds.hasNext());
    }

    @Override
    @Transactional
    public void deleteReportedPost(UUID postId, String adminUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User admin = requireUser(adminUsername);

        long reportCount = postReportRepository.countByPostId(postId);
        notificationService.notifyPostDeletedByReport(post, admin, reportCount);
        postReportRepository.deleteByPostId(postId);
        postDeletionService.hardDeletePost(post);
    }

    private Post getPendingPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!Boolean.TRUE.equals(post.getIsPending())) {
            throw new IllegalArgumentException("Post is not pending");
        }
        return post;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private List<AdminPendingPostItem> buildPendingPostItems(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        List<Media> mediaList = mediaRepository.findByPostIdInOrderByOrderIndexAsc(postIds);

        Map<UUID, List<Media>> mediaByPostId = mediaList.stream()
                .collect(Collectors.groupingBy(m -> m.getPost().getId(), LinkedHashMap::new, Collectors.toList()));

        List<UUID> mediaIds = mediaList.stream().map(Media::getId).toList();
        Map<UUID, MediaVariant> originalByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.ORIGINAL)
                .stream()
                .collect(Collectors.toMap(v -> v.getMedia().getId(), v -> v));
        Map<UUID, MediaVariant> thumbnailByMediaId = mediaVariantRepository
                .findByMediaIdInAndType(mediaIds, MediaVariantType.THUMBNAIL)
                .stream()
                .collect(Collectors.toMap(v -> v.getMedia().getId(), v -> v));

        Map<UUID, List<String>> tagsByPostId = postTagRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getPost().getId(),
                        Collectors.mapping(pt -> pt.getTag().getName(), Collectors.toList())
                ));

        Map<UUID, Long> likeCountByPostId = postLikeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostLikeRepository.PostLikeCountView::getPostId,
                        PostLikeRepository.PostLikeCountView::getLikeCount));

        Map<UUID, Long> commentCountByPostId = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(CommentRepository.PostCommentCountView::getPostId,
                        CommentRepository.PostCommentCountView::getCommentCount));

        List<UUID> userIds = posts.stream().map(p -> p.getUser().getId()).distinct().toList();
        Map<UUID, String> avatarByUserId = loadAvatarUrlsForUsers(userIds);

        return posts.stream()
                .map(post -> new AdminPendingPostItem(
                        post.getId(),
                        post.getUser().getId(),
                        post.getUser().getDisplayName(),
                        post.getUser().getUsername(),
                        avatarByUserId.get(post.getUser().getId()),
                        post.getCaption(),
                        post.getCreatedAt(),
                        toMediaResponses(
                                mediaByPostId.getOrDefault(post.getId(), Collections.emptyList()),
                                originalByMediaId,
                                thumbnailByMediaId
                        ),
                        tagsByPostId.getOrDefault(post.getId(), List.of()),
                        commentCountByPostId.getOrDefault(post.getId(), 0L),
                        likeCountByPostId.getOrDefault(post.getId(), 0L),
                        post.getFlaggedMatchedPostId(),
                        post.getFlaggedMatchedAuthorDisplayName()
                ))
                .toList();
    }

    private List<MediaItemResponse> toMediaResponses(
            List<Media> mediaList,
            Map<UUID, MediaVariant> originalByMediaId,
            Map<UUID, MediaVariant> thumbnailByMediaId
    ) {
        return mediaList.stream()
                .map(media -> {
                    MediaVariant original = originalByMediaId.get(media.getId());
                    MediaVariant thumbnail = thumbnailByMediaId.get(media.getId());
                    return new MediaItemResponse(
                            media.getId(),
                            media.getOrderIndex(),
                            original != null ? original.getPublicId() : null,
                            original != null ? original.getUrl() : null,
                            thumbnail != null ? thumbnail.getPublicId() : null,
                            thumbnail != null ? thumbnail.getUrl() : null
                    );
                })
                .toList();
    }

    private Map<UUID, String> loadAvatarUrlsForUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> avatarUrlByUserId = new java.util.HashMap<>();
        for (Profile profile : profileRepository.findByUserIdIn(userIds)) {
            if (profile.getUser() != null) {
                avatarUrlByUserId.put(profile.getUser().getId(), profile.getAvatarUrl());
            }
        }
        return avatarUrlByUserId;
    }
}

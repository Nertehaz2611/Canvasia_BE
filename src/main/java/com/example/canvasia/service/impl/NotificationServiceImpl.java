package com.example.canvasia.service.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.canvasia.dto.notification.NotificationFeedResponse;
import com.example.canvasia.dto.notification.NotificationResponse;
import com.example.canvasia.entity.Comment;
import com.example.canvasia.entity.Notification;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.NotificationType;
import com.example.canvasia.enums.ReferenceType;
import com.example.canvasia.repository.CommentRepository;
import com.example.canvasia.repository.NotificationRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public NotificationFeedResponse getNotifications(String username, int page, int size) {
        User user = requireUser(username);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<Notification> notifications = notificationRepository.findByUserUsernameOrderByCreatedAtDesc(user.getUsername(), pageable);
        List<NotificationResponse> items = mapNotifications(notifications.getContent());

        return new NotificationFeedResponse(
                items,
                notificationRepository.countByUserUsernameAndIsReadFalse(user.getUsername()),
                safePage,
                safeSize,
                notifications.hasNext()
        );
    }

    @Override
    @Transactional
    public void markAsRead(String username, UUID notificationId) {
        if (notificationRepository.markAsRead(username, notificationId) == 0) {
            throw new IllegalArgumentException("Notification not found or access denied");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsRead(username);
    }

    @Override
    @Transactional
    public void notifyPostLike(Post post, User actor) {
        if (post == null || post.getUser() == null || actor == null) {
            return;
        }

        createAndPublish(
                post.getUser(),
                actor,
                NotificationType.POST_LIKE,
                ReferenceType.POST,
                post.getId(),
                post.getId(),
                actor.getDisplayName() + " liked your post"
        );
    }

    @Override
    @Transactional
    public void notifyCommentLike(Comment comment, User actor) {
        if (comment == null || comment.getUser() == null || comment.getId() == null || actor == null) {
            return;
        }

        createAndPublish(
                comment.getUser(),
                actor,
                NotificationType.COMMENT_LIKE,
                ReferenceType.COMMENT,
                comment.getId(),
                comment.getPost() != null ? comment.getPost().getId() : null,
                actor.getDisplayName() + " liked your comment"
        );
    }

    @Override
    @Transactional
    public void notifyFollow(User followedUser, User actor) {
        if (followedUser == null || followedUser.getId() == null || actor == null) {
            return;
        }

        createAndPublish(
                followedUser,
                actor,
                NotificationType.FOLLOW,
                ReferenceType.USER,
                actor.getId(),
                null,
                actor.getDisplayName() + " started following you"
        );
    }

    @Override
    @Transactional
    public void notifyPostComment(Post post, User actor) {
        if (post == null || post.getUser() == null || actor == null) {
            return;
        }

        createAndPublish(
                post.getUser(),
                actor,
                NotificationType.POST_COMMENT,
                ReferenceType.POST,
                post.getId(),
                post.getId(),
                actor.getDisplayName() + " commented on your post"
        );
    }

    @Override
    @Transactional
    public void notifyRootCommentReply(Comment rootComment, User actor) {
        if (rootComment == null || rootComment.getUser() == null || actor == null || rootComment.getId() == null) {
            return;
        }

        Set<UUID> recipientIds = new LinkedHashSet<>(commentRepository.findParticipantUserIdsByRootId(rootComment.getId()));
        if (recipientIds.isEmpty()) {
            recipientIds.add(rootComment.getUser().getId());
        }

        Map<UUID, User> recipientsById = userRepository.findAllById(recipientIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        for (User recipient : recipientsById.values()) {
            if (recipient == null || recipient.getId() == null || recipient.getId().equals(actor.getId())) {
                continue;
            }

            NotificationType type = recipient.getId().equals(rootComment.getUser().getId())
                    ? NotificationType.COMMENT_REPLY
                    : NotificationType.COMMENT_THREAD_REPLY;

            String content = recipient.getId().equals(rootComment.getUser().getId())
                    ? actor.getDisplayName() + " replied to your comment"
                    : actor.getDisplayName() + " replied in a thread you participated in";

            createAndPublish(
                    recipient,
                    actor,
                    type,
                    ReferenceType.COMMENT,
                    rootComment.getId(),
                    rootComment.getPost() != null ? rootComment.getPost().getId() : null,
                    content
            );
        }
    }

    @Override
    @Transactional
    public void notifyPostPending(Post post) {
        if (post == null || post.getUser() == null) {
            return;
        }

        createAndPublish(
                post.getUser(),
                null,
                NotificationType.POST_PENDING,
                ReferenceType.POST,
                post.getId(),
                post.getId(),
            "Your post is pending moderation review"
        );
    }

    @Override
    @Transactional
    public void notifyPostApproved(Post post, User actor) {
        if (post == null || post.getUser() == null) {
            return;
        }

        createAndPublish(
                post.getUser(),
                actor,
                NotificationType.POST_APPROVED,
                ReferenceType.POST,
                post.getId(),
                post.getId(),
                actor != null
                    ? actor.getDisplayName() + " approved your post"
                    : "Your post was approved"
        );
    }

    @Override
    @Transactional
    public void notifyPostDeleted(Post post, User actor) {
        if (post == null || post.getUser() == null) {
            return;
        }

        createAndPublish(
                post.getUser(),
                actor,
                NotificationType.POST_DELETED,
                ReferenceType.POST,
                post.getId(),
                post.getId(),
                actor != null
                    ? actor.getDisplayName() + " deleted your post"
                    : "Your post was deleted"
        );
    }

    private void createAndPublish(
            User recipient,
            User actor,
            NotificationType type,
            ReferenceType referenceType,
            UUID referenceId,
            UUID postId,
            String content
    ) {
        if (recipient == null || recipient.getId() == null || referenceId == null) {
            return;
        }

        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = Notification.create(
                type,
                referenceType,
                referenceId,
                content,
                recipient,
                actor
        );

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved, postId);
        publishAfterCommit(recipient.getUsername(), response);
    }

    private List<NotificationResponse> mapNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        Set<UUID> actorIds = new HashSet<>();
        Set<UUID> commentReferenceIds = new HashSet<>();
        for (Notification notification : notifications) {
            if (notification.getActor() != null && notification.getActor().getId() != null) {
                actorIds.add(notification.getActor().getId());
            }
            if (notification.getReferenceType() == ReferenceType.COMMENT && notification.getReferenceId() != null) {
                commentReferenceIds.add(notification.getReferenceId());
            }
        }

        Map<UUID, Profile> profileMap = profileRepository.findByUserIdIn(actorIds).stream()
                .filter(profile -> profile.getUser() != null && profile.getUser().getId() != null)
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), profile -> profile, (left, right) -> left));

        Map<UUID, UUID> postIdByCommentId = commentReferenceIds.isEmpty()
                ? Map.of()
                : commentRepository.findPostIdsByCommentIds(commentReferenceIds).stream()
                        .collect(Collectors.toMap(
                                CommentRepository.CommentPostRefView::getCommentId,
                                CommentRepository.CommentPostRefView::getPostId,
                                (left, right) -> left
                        ));

        return notifications.stream()
                .map(notification -> toResponse(
                        notification,
                resolvePostId(notification, postIdByCommentId),
                profileMap.get(resolveActorId(notification))
                ))
                .toList();
    }

    private UUID resolvePostId(Notification notification, Map<UUID, UUID> postIdByCommentId) {
        if (notification.getReferenceType() == ReferenceType.POST) {
            return notification.getReferenceId();
        }
        if (notification.getReferenceType() == ReferenceType.COMMENT) {
            return postIdByCommentId.get(notification.getReferenceId());
        }
        return null;
    }

    private NotificationResponse toResponse(Notification notification, UUID postId) {
        Profile actorProfile = null;
        if (notification.getActor() != null && notification.getActor().getId() != null) {
            actorProfile = profileRepository.findByUserId(notification.getActor().getId()).orElse(null);
        }
        return toResponse(notification, postId, actorProfile);
    }

    private NotificationResponse toResponse(Notification notification, UUID postId, Profile actorProfile) {
        User actor = notification.getActor();
        String actorDisplayName = null;
        if (actorProfile != null) {
            actorDisplayName = actorProfile.getDisplayName();
        } else if (actor != null) {
            actorDisplayName = actor.getDisplayName();
        }

        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getReferenceType().name(),
                notification.getReferenceId(),
                postId,
                notification.getContent(),
                Boolean.TRUE.equals(notification.getIsRead()),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getUsername() : null,
                actorDisplayName,
                actorProfile != null ? actorProfile.getAvatarUrl() : null,
                notification.getCreatedAt()
        );
    }

    private void publishAfterCommit(String username, NotificationResponse response) {
        Consumer<NotificationResponse> publisher = item -> messagingTemplate.convertAndSendToUser(username, "/queue/notifications", item);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.accept(response);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.accept(response);
            }
        });
    }

    private UUID resolveActorId(Notification notification) {
        if (notification == null || notification.getActor() == null) {
            return null;
        }
        return notification.getActor().getId();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
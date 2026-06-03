package com.example.canvasia.service.interfaces;

import java.util.UUID;

import com.example.canvasia.dto.notification.NotificationFeedResponse;
import com.example.canvasia.entity.Comment;
import com.example.canvasia.entity.Post;
import com.example.canvasia.entity.User;

public interface NotificationService {

    NotificationFeedResponse getNotifications(String username, int page, int size);

    void markAsRead(String username, UUID notificationId);

    void markAllAsRead(String username);

    void notifyPostLike(Post post, User actor);

    void notifyCommentLike(Comment comment, User actor);

    void notifyFollow(User followedUser, User actor);

    void notifyPostComment(Post post, User actor);

    void notifyRootCommentReply(Comment rootComment, User actor);

    void notifyPostPending(Post post);

    void notifyPostApproved(Post post, User actor);

    void notifyPostRejected(Post post, User actor);

    void notifyPostDeleted(Post post, User actor);

    void notifyPostDeletedByReport(Post post, User actor, long reportCount);
}
package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.exception.DomainValidationException;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(columnList = "post_id"),
                @Index(columnList = "parent_id"),
                @Index(columnList = "root_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Comment extends AuditableEntity {

    @Column(name = "root_id")
    private UUID rootId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private Comment parent;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    public static Comment createRootComment(Post post, User user, String content) {
        validate(post, user, content);
        if (content.isBlank()) {
            throw new DomainValidationException("COMMENT_CONTENT_BLANK", "Content must not be blank");
        }

        return Comment.builder()
                .rootId(null)
                .parent(null)
                .content(content)
                .user(user)
                .post(post)
                .build();
    }

    public static Comment createReplyComment(Comment parent, User user, String content) {
        validate(parent, user, content);
        if (content.isBlank()) {
            throw new DomainValidationException("COMMENT_CONTENT_BLANK", "Content must not be blank");
        }

        UUID effectiveRootId = (parent.getRootId() == null) ? parent.getId() : parent.getRootId();

        return Comment.builder()
                .rootId(effectiveRootId)
                .parent(parent)
                .content(content)
                .user(user)
                .post(parent.getPost())
                .build();
    }

    public void updateContent(String newContent) {
        validate(newContent);
        if (newContent.isBlank()) {
            throw new DomainValidationException("COMMENT_CONTENT_BLANK", "New content must not be blank");
        }
        this.content = newContent;
    }
}

package com.example.canvasia.entity;

import java.util.UUID;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.enums.NotificationType;
import com.example.canvasia.enums.ReferenceType;
import com.example.canvasia.exception.DomainValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(columnList = "user_id"),
                @Index(columnList = "is_read")
        }
)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Notification extends AuditableEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private UUID referenceId;

    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", updatable = false)
    @ToString.Exclude
    private User actor;

    public static Notification create(
            NotificationType type,
            ReferenceType referenceType,
            UUID referenceId,
            String content,
            User user,
            User actor
    ) {
        validate(type, referenceType, referenceId, user);
        validateBusinessRules(type, referenceType, user, actor);

        return Notification.builder()
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .content(content)
                .user(user)
                .actor(actor)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public NotificationType getType() {
        return type;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getContent() {
        return content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public User getUser() {
        return user;
    }

    public User getActor() {
        return actor;
    }

    private static void validateBusinessRules(
            NotificationType type,
            ReferenceType referenceType,
            User user,
            User actor
    ) {
        switch (type) {
            case POST_LIKE, POST_COMMENT -> {
                if (referenceType != ReferenceType.POST) {
                    throw new DomainValidationException(
                            "NOTIFICATION_REFERENCE_TYPE_INVALID",
                            type + " notification must reference POST"
                    );
                }
            }
            case COMMENT_LIKE, COMMENT_REPLY, COMMENT_THREAD_REPLY -> {
                if (referenceType != ReferenceType.COMMENT) {
                    throw new DomainValidationException(
                            "NOTIFICATION_REFERENCE_TYPE_INVALID",
                            type + " notification must reference COMMENT"
                    );
                }
            }
            case POST_PENDING, POST_APPROVED, POST_REJECTED, POST_DELETED, POST_DELETED_REPORTED -> {
                if (referenceType != ReferenceType.POST) {
                    throw new DomainValidationException(
                            "NOTIFICATION_REFERENCE_TYPE_INVALID",
                            type + " notification must reference POST"
                    );
                }
            }
            case FOLLOW -> {
                if (referenceType != ReferenceType.USER) {
                    throw new DomainValidationException(
                            "NOTIFICATION_REFERENCE_TYPE_INVALID",
                            "FOLLOW notification must reference USER"
                    );
                }
            }
            default -> throw new DomainValidationException(
                    "NOTIFICATION_TYPE_UNSUPPORTED",
                    "Unsupported notification type: " + type
            );
        }

        boolean actorRequired = switch (type) {
            case POST_PENDING, POST_APPROVED, POST_REJECTED, POST_DELETED, POST_DELETED_REPORTED -> false;
            default -> true;
        };

        if (actorRequired && actor == null) {
            throw new DomainValidationException(
                    "NOTIFICATION_ACTOR_REQUIRED",
                    "Actor is required for social notifications"
            );
        }

        if (actor != null && actor.equals(user)) {
            throw new DomainValidationException(
                    "NOTIFICATION_SELF_TARGET_NOT_ALLOWED",
                    "Self-notification is not allowed"
            );
        }
    }
}


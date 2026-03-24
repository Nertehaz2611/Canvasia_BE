package com.example.canvasia.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

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

    private static void validateBusinessRules(
            NotificationType type,
            ReferenceType referenceType,
            User user,
            User actor
    ) {
        switch (type) {
            case LIKE, COMMENT -> {
                if (referenceType != ReferenceType.POST && referenceType != ReferenceType.COMMENT) {
                    throw new DomainValidationException(
                            "NOTIFICATION_REFERENCE_TYPE_INVALID",
                            type + " notification must reference POST or COMMENT"
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

        if (actor == null) {
            throw new DomainValidationException(
                    "NOTIFICATION_ACTOR_REQUIRED",
                    "Actor is required for social notifications"
            );
        }

        if (actor.equals(user)) {
            throw new DomainValidationException(
                    "NOTIFICATION_SELF_TARGET_NOT_ALLOWED",
                    "Self-notification is not allowed"
            );
        }
    }
}


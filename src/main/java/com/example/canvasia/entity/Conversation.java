package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participant1_id", "participant2_id"}),
        indexes = {
                @Index(name = "idx_conv_p1", columnList = "participant1_id"),
                @Index(name = "idx_conv_p2", columnList = "participant2_id"),
                @Index(name = "idx_conv_last_msg", columnList = "last_message_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PUBLIC)
public class Conversation extends AuditableEntity {

    /**
     * participant1_id < participant2_id (UUID ordering) to avoid duplicates.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false, updatable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false, updatable = false)
    private User participant2;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    @Column(name = "last_sender_id")
    private UUID lastSenderId;

    @Column(name = "participant1_unread", nullable = false)
    @Builder.Default
    private int participant1Unread = 0;

    @Column(name = "participant2_unread", nullable = false)
    @Builder.Default
    private int participant2Unread = 0;

    public static Conversation create(User a, User b) {
        // Always store smaller UUID as participant1 to guarantee uniqueness
        if (a.getId().compareTo(b.getId()) <= 0) {
            return Conversation.builder().participant1(a).participant2(b).build();
        } else {
            return Conversation.builder().participant1(b).participant2(a).build();
        }
    }

    public int getUnreadCountFor(UUID userId) {
        if (userId.equals(participant1.getId())) return participant1Unread;
        if (userId.equals(participant2.getId())) return participant2Unread;
        return 0;
    }

    public User getOtherParticipant(UUID myUserId) {
        return myUserId.equals(participant1.getId()) ? participant2 : participant1;
    }

    public void applyNewMessage(String content, UUID senderId) {
        this.lastMessageAt = LocalDateTime.now();
        this.lastMessagePreview = content.length() > 200 ? content.substring(0, 200) : content;
        this.lastSenderId = senderId;
        if (senderId.equals(participant1.getId())) {
            this.participant2Unread++;
        } else {
            this.participant1Unread++;
        }
    }

    public void markReadFor(UUID userId) {
        if (userId.equals(participant1.getId())) {
            this.participant1Unread = 0;
        } else if (userId.equals(participant2.getId())) {
            this.participant2Unread = 0;
        }
    }
}

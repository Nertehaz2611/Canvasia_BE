package com.example.canvasia.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.canvasia.entity.base.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.ToString;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Post extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isPending = false;

    @Column
    private LocalDateTime deletedAt;

    @Column(name = "flagged_matched_post_id")
    private UUID flaggedMatchedPostId;

    @Column(name = "flagged_matched_author_display_name", length = 100)
    private String flaggedMatchedAuthorDisplayName;

    public static Post create(User user, String caption) {
        validate(user);

        return Post.builder()
                .user(user)
                .caption(caption)
                .build();
    }

    public void updateCaption(String newCaption) {
        this.caption = newCaption;
    }

    public void markPending(boolean pending) {
        this.isPending = pending;
    }

    public void flagWith(UUID matchedPostId, String authorDisplayName) {
        this.flaggedMatchedPostId = matchedPostId;
        this.flaggedMatchedAuthorDisplayName = authorDisplayName;
    }

    public void moveToTrash() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restoreFromTrash() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}

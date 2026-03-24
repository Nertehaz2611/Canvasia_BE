package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "media",
        uniqueConstraints = @UniqueConstraint(columnNames ={"post_id", "order_index"}),
        indexes = {
                @Index(columnList = "post_id"),
                @Index(columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Media extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private MediaType type;

    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    public static Media create(Post post, MediaType type, int orderIndex) {
        validate(post, type, orderIndex);

        return Media.builder()
                .type(type)
                .orderIndex(orderIndex)
                .post(post)
                .userId(post.getUser().getId())
                .build();
    }

    public void updateOrder(int newIndex) {
        this.orderIndex = newIndex;
    }

    @PrePersist
    @PreUpdate
    private void ensureOwnerConsistency() {
        if (post == null || post.getUser() == null || post.getUser().getId() == null) {
            throw new IllegalStateException("Post owner must exist before persisting media");
        }

        UUID expectedUserId = post.getUser().getId();
        if (userId == null) {
            userId = expectedUserId;
            return;
        }

        if (!userId.equals(expectedUserId)) {
            throw new IllegalStateException("Media userId must match post owner id");
        }
    }
}

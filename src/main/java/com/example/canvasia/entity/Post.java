package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

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

    public static Post create(User user, String caption) {
        validate(user);

        return Post.builder()
                .user(user)
                .caption(caption)
                .build();
    }

    public void moveToTrash() {
            this.isDeleted = true;
    }

    public void restoreFromTrash() {
            this.isDeleted = false;
    }
}

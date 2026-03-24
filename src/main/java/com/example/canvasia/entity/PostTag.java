package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "post_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"}),
        indexes = {
                @Index(columnList = "post_id"),
                @Index(columnList = "tag_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PostTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    @ToString.Exclude
    private Tag tag;

    public static PostTag create(Post post, Tag tag) {
            validate(post, tag);

            return PostTag.builder()
                            .post(post)
                            .tag(tag)
                            .build();
    }
}

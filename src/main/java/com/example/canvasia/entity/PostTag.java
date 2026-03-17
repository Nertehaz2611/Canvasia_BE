package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "post_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"}),
        indexes = {
                @Index(columnList = "post_id"),
                @Index(columnList = "tag_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PostTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    @ToString.Exclude
    private Tag tag;
}

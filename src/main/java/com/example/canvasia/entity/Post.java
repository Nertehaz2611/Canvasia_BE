package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Post extends AuditableEntity {

    @Column(columnDefinition = "TEXT")
    private String caption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Media> mediaList;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade =  CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PostTag> postTags;
}

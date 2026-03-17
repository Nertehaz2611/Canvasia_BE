package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "media",
        uniqueConstraints = @UniqueConstraint(columnNames ={"post_id", "order_index"})
)
@Getter
@Setter
@NoArgsConstructor
public class Media extends BaseEntity {

    private String url;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @ToString.Exclude
    private Post post;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<MediaVariant> variants;
}

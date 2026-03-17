package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.enums.MediaVariantType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "media_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"media_id", "type"})
)
@Getter
@Setter
@NoArgsConstructor
public class MediaVariant extends BaseEntity {

    private String url;

    @Enumerated(EnumType.STRING)
    private MediaVariantType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    @ToString.Exclude
    private Media media;
}

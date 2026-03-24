package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.exception.DomainValidationException;
import com.example.canvasia.enums.MediaVariantType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "media_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"media_id", "type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MediaVariant extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MediaVariantType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false, updatable = false)
    @ToString.Exclude
    private Media media;

    public static MediaVariant create(String url, MediaVariantType type, Media media) {
        validate(url, type, media);
        if (url.isBlank()) {
            throw new DomainValidationException("MEDIA_VARIANT_URL_BLANK", "Image url must not be blank");
        }

        return MediaVariant.builder()
                .url(url)
                .type(type)
                .media(media)
                .build();
    }
}

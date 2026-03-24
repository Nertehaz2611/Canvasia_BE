package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "portfolio_media",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "media_id"}),
        indexes = {
                @Index(columnList = "portfolio_id"),
                @Index(columnList = "media_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PortfolioMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @ToString.Exclude
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    @ToString.Exclude
    private Media media;

    public static PortfolioMedia create(Portfolio portfolio, Media media) {
            validate(portfolio, media);

            return PortfolioMedia.builder()
                            .portfolio(portfolio)
                            .media(media)
                            .build();
    }
}

package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.exception.DomainValidationException;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
        name = "follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}),
        indexes = {
                @Index(name = "idx_follower", columnList = "follower_id"),
                @Index(name = "idx_following", columnList = "following_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Follow extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false, updatable = false)
    @ToString.Exclude
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false, updatable = false)
    @ToString.Exclude
    private User following;

    public static Follow create(User follower, User following) {
        validate(follower, following);
        if (follower.equals(following)) {
            throw new DomainValidationException("FOLLOW_SELF_NOT_ALLOWED", "Cannot follow yourself");
        }

        return Follow.builder()
                .follower(follower)
                .following(following)
                .build();
    }
}
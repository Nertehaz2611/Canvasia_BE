package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.exception.DomainValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "profiles",
        indexes = @Index(columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Profile extends BaseEntity {

    @Column(nullable = false, length = 25)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 120)
    private String avatarPublicId;

    private String avatarUrl;

    private String website;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;

    public static Profile create(User user, String displayName) {
        validate(user, displayName);
        if (displayName.isBlank()) {
            throw new DomainValidationException("PROFILE_DISPLAY_NAME_BLANK", "Display name must not be blank");
        }

        return Profile.builder()
                .displayName(displayName)
                .user(user)
                .build();
    }

    public void updateDisplayName(String newDisplayName) {
        validate(newDisplayName);
        if (newDisplayName.isBlank()) {
            throw new DomainValidationException("PROFILE_DISPLAY_NAME_BLANK", "Display name must not be blank");
        }
        this.displayName = newDisplayName;
    }

    public void updateBio(String newBio) {
        this.bio = newBio;
    }

    public void updateAvatarUrl(String newAvatarUrl) {
        this.avatarUrl = newAvatarUrl;
    }

    public void updateAvatarPublicId(String newAvatarPublicId) {
        this.avatarPublicId = newAvatarPublicId;
    }

    public void updateWebsite(String newWebsite) {
        this.website = newWebsite;
    }
}

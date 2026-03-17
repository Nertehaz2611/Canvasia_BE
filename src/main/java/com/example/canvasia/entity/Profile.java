package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "profiles",
        indexes = @Index(columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BaseEntity {

    @Column(nullable = false, length = 25)
    private String displayName;

    @Column(nullable = false, length = 25)
    private String username;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String avatarUrl;

    private String website;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;
}

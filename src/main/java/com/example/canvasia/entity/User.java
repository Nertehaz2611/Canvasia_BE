package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.exception.DomainValidationException;
import com.example.canvasia.enums.UserRole;
import com.example.canvasia.enums.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        name = "users",
        indexes = {
                @Index(columnList = "status"),
                @Index(columnList = "role")
        }
)
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class User extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 25)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Profile profile;

    public static User create(String username, String email, String passwordHash) {
        validate(username, email, passwordHash);
        if (username.isBlank() || email.isBlank() || passwordHash.isBlank()) {
            throw new DomainValidationException(
                    "USER_CREDENTIAL_FIELDS_BLANK",
                    "Username, email and password hash must not be blank"
            );
        }

        return User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void updateStatus(UserStatus newStatus) {
        validate(newStatus);
        this.status = newStatus;
    }

    public void updateRole(UserRole newRole) {
        validate(newRole);
        this.role = newRole;
    }

    public void changePasswordHash(String newPasswordHash) {
        validate(newPasswordHash);
        if (newPasswordHash.isBlank()) {
            throw new DomainValidationException("USER_PASSWORD_HASH_BLANK", "Password hash must not be blank");
        }
        this.passwordHash = newPasswordHash;
    }
}

package com.example.canvasia.entity;

import com.example.canvasia.entity.base.AuditableEntity;
import com.example.canvasia.exception.DomainValidationException;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "portfolios",
        indexes = {
                @Index(columnList = "user_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Portfolio extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    private String name;

    public static Portfolio create(User user, String name) {
            validate(user, name);
            if (name.isBlank()) {
                    throw new DomainValidationException("PORTFOLIO_NAME_BLANK", "Portfolio name must not be blank");
    }

            return Portfolio.builder()
                            .user(user)
                            .name(name)
                            .build();
    }

    public void updateName(String newName) {
            validate(newName);
            if (newName.isBlank()) {
                    throw new DomainValidationException("PORTFOLIO_NAME_BLANK", "Portfolio name must not be blank");
    }
            this.name = newName;
    }
}

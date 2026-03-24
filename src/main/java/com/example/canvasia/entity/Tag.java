package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.exception.DomainValidationException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Tag extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    public static Tag create(String name) {
        validate(name);
        if (name.isBlank()) {
            throw new DomainValidationException("TAG_NAME_BLANK", "Tag name must not be blank");
        }

        return Tag.builder()
                .name(name)
                .build();
    }

    public void rename(String newName) {
        validate(newName);
        if (newName.isBlank()) {
            throw new DomainValidationException("TAG_NAME_BLANK", "Tag name must not be blank");
        }
        this.name = newName;
    }
}
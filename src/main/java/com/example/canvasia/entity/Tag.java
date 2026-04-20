package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import com.example.canvasia.enums.TagType;
import com.example.canvasia.exception.DomainValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "type"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Tag extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType type;

    public static Tag create(String name, TagType type) {
        validate(name, type);
        if (name.isBlank()) {
            throw new DomainValidationException("TAG_NAME_BLANK", "Tag name must not be blank");
        }

        return Tag.builder()
                .name(name)
                .type(type)
                .build();
    }

    public void rename(String newName) {
        validate(newName);
        if (newName.isBlank()) {
            throw new DomainValidationException("TAG_NAME_BLANK", "Tag name must not be blank");
        }
        this.name = newName;
    }

    public void updateType(TagType newType) {
        validate(newType);
        this.type = newType;
    }
}
package com.example.canvasia.entity.base;

import com.example.canvasia.exception.DomainValidationException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    protected static void validate(Object... objects) {
        if (objects == null) {
            throw new DomainValidationException(
                    "VALIDATION_INPUT_ARRAY_NULL",
                    "The array of objects must not be null"
            );
        }
        for (Object obj : objects) {
            if (obj == null) {
                throw new DomainValidationException(
                        "VALIDATION_INPUT_NULL",
                        "This object must not be null"
                );
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        BaseEntity that = (BaseEntity) o;

        if (id == null || that.id == null) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : getClass().hashCode();
    }
}

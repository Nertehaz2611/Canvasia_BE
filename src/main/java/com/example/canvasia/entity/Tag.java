package com.example.canvasia.entity;

import com.example.canvasia.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;
}
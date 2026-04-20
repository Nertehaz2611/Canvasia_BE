package com.example.canvasia.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.canvasia.entity.Tag;
import com.example.canvasia.enums.TagType;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNameAndType(String name, TagType type);
}

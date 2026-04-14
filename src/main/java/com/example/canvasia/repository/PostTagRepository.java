package com.example.canvasia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.canvasia.entity.PostTag;

public interface PostTagRepository extends JpaRepository<PostTag, UUID> {

	List<PostTag> findByPostIdIn(List<UUID> postIds);

    List<PostTag> findByPostId(UUID postId);
}

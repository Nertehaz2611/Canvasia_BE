package com.example.canvasia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.PostTag;
import com.example.canvasia.enums.TagType;

public interface PostTagRepository extends JpaRepository<PostTag, UUID> {

	List<PostTag> findByPostIdIn(List<UUID> postIds);

    List<PostTag> findByPostId(UUID postId);

	@Query("""
		select t.name
		from PostTag pt
		join pt.tag t
		join pt.post p
		where p.isDeleted = false
		  and t.type = :tagType
		group by t.id, t.name
		order by max(p.createdAt) desc
		""")
	List<String> findLatestTagNames(@Param("tagType") TagType tagType, Pageable pageable);
}

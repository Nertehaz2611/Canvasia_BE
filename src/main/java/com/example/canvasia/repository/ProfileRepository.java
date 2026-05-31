package com.example.canvasia.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);

    @Query("""
        select p from Profile p
        join fetch p.user
        where p.user.id in :userIds
        """)
    List<Profile> findByUserIdIn(@Param("userIds") Collection<UUID> userIds);
}

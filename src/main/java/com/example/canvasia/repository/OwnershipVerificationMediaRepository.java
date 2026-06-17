package com.example.canvasia.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.OwnershipVerificationMedia;

public interface OwnershipVerificationMediaRepository extends JpaRepository<OwnershipVerificationMedia, UUID> {

    List<OwnershipVerificationMedia> findByOwnershipVerificationId(UUID ownershipVerificationId);

    @Query("SELECT ov.user.id FROM OwnershipVerificationMedia ovm JOIN ovm.ownershipVerification ov WHERE ovm.id = :mediaId")
    Optional<UUID> findOwnerUserIdByMediaId(@Param("mediaId") UUID mediaId);

    @Query("SELECT ov.post.id FROM OwnershipVerificationMedia ovm JOIN ovm.ownershipVerification ov WHERE ovm.id = :mediaId")
    Optional<UUID> findOwnerPostIdByMediaId(@Param("mediaId") UUID mediaId);

    @Query("SELECT ov.user.displayName FROM OwnershipVerificationMedia ovm JOIN ovm.ownershipVerification ov WHERE ovm.id = :mediaId")
    Optional<String> findOwnerDisplayNameByMediaId(@Param("mediaId") UUID mediaId);
}

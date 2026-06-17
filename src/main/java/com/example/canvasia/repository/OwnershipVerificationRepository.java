package com.example.canvasia.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.canvasia.entity.OwnershipVerification;
import com.example.canvasia.enums.OwnershipVerificationStatus;

@Repository
public interface OwnershipVerificationRepository extends JpaRepository<OwnershipVerification, UUID> {

    Optional<OwnershipVerification> findByPostId(UUID postId);

    Optional<OwnershipVerification> findByUserIdAndPostId(UUID userId, UUID postId);

    List<OwnershipVerification> findByUserId(UUID userId);

    Page<OwnershipVerification> findByStatus(OwnershipVerificationStatus status, Pageable pageable);

    Page<OwnershipVerification> findAll(Pageable pageable);

    @Query("SELECT ov FROM OwnershipVerification ov WHERE ov.status = :status ORDER BY ov.createdAt ASC")
    Page<OwnershipVerification> findPendingVerifications(@Param("status") OwnershipVerificationStatus status, Pageable pageable);

    long countByStatus(OwnershipVerificationStatus status);

    List<OwnershipVerification> findByStatusAndUserId(OwnershipVerificationStatus status, UUID userId);

    @Query("SELECT ov FROM OwnershipVerification ov WHERE ov.id = :verificationId")
    Optional<OwnershipVerification> findByIdWithUserEager(@Param("verificationId") UUID verificationId);

    @Query("SELECT ov FROM OwnershipVerification ov WHERE ov.post.id = :postId")
    Optional<OwnershipVerification> findByPostIdWithUserEager(@Param("postId") UUID postId);

    @Query(value = "SELECT user_id FROM ownership_verifications WHERE id = :verificationId", nativeQuery = true)
    Optional<UUID> findUserIdByVerificationId(@Param("verificationId") UUID verificationId);

    @Query(value = "SELECT user_id FROM ownership_verifications WHERE post_id = :postId", nativeQuery = true)
    Optional<UUID> findUserIdByPostId(@Param("postId") UUID postId);
}

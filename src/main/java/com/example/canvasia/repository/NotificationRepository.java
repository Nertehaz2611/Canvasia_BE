package com.example.canvasia.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    long countByUserUsernameAndIsReadFalse(String username);

    @Modifying
    @Query("""
        update Notification n
        set n.isRead = true
        where n.id = :notificationId
          and n.user.username = :username
        """)
    int markAsRead(@Param("username") String username, @Param("notificationId") UUID notificationId);

    @Modifying
    @Query("""
        update Notification n
        set n.isRead = true
        where n.user.username = :username
          and n.isRead = false
        """)
    int markAllAsRead(@Param("username") String username);
}
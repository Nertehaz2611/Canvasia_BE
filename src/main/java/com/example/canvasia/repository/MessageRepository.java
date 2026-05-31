package com.example.canvasia.repository;

import com.example.canvasia.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * First page (no cursor): fetch most recent messages, most-recent-first.
     * NOTE: No JOIN FETCH here intentionally — Hibernate 6 throws when JOIN FETCH
     * is combined with Pageable (pagination). Sender is lazy-loaded within the
     * caller's @Transactional session.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :convId
            ORDER BY m.createdAt DESC
            """)
    List<Message> findFirstPageByConversation(
            @Param("convId") UUID convId,
            Pageable pageable);

    /**
     * Cursor page: fetch messages older than cursor, most-recent-first.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :convId
              AND m.createdAt < :cursor
            ORDER BY m.createdAt DESC
            """)
    List<Message> findPageByConversationBefore(
            @Param("convId") UUID convId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    /**
     * Mark all unread messages in a conversation as read for a given recipient.
     */
    @Modifying
    @Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.conversation.id = :convId
              AND m.sender.id <> :readerId
              AND m.readAt IS NULL
            """)
    void markConversationReadFor(
            @Param("convId") UUID convId,
            @Param("readerId") UUID readerId,
            @Param("now") LocalDateTime now);
}

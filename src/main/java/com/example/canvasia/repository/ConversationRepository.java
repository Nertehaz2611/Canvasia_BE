package com.example.canvasia.repository;

import com.example.canvasia.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.participant1.id = :a AND c.participant2.id = :b)
               OR (c.participant1.id = :b AND c.participant2.id = :a)
            """)
    Optional<Conversation> findByParticipants(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.participant1
            JOIN FETCH c.participant2
            WHERE c.participant1.id = :userId OR c.participant2.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    List<Conversation> findAllByParticipant(@Param("userId") UUID userId);
}

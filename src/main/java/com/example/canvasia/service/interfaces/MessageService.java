package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.message.ConversationResponse;
import com.example.canvasia.dto.message.MessagePageResponse;
import com.example.canvasia.dto.message.MessageResponse;
import com.example.canvasia.dto.message.SendMessageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageService {

    /**
     * Send a message. Creates conversation if it doesn't exist yet.
     * Pushes the result via STOMP to both participants.
     */
    MessageResponse sendMessage(String senderUsername, SendMessageRequest request);

    /**
     * Fetch paginated message history for a conversation (newest-first cursor).
     */
    MessagePageResponse getMessages(String callerUsername, UUID conversationId,
                                    LocalDateTime cursor, int limit);

    /**
     * Get the caller's conversation list sorted by lastMessageAt desc.
     */
    List<ConversationResponse> getConversations(String callerUsername);

    /**
     * Mark all messages in a conversation as read; returns updated unread count (0).
     */
    void markRead(String callerUsername, UUID conversationId);

    /**
     * Get or create a DM conversation with another user.
     */
    ConversationResponse getOrCreateConversation(String callerUsername, String otherUsername);
}

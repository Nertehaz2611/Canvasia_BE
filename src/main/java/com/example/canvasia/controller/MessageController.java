package com.example.canvasia.controller;

import com.example.canvasia.dto.message.ConversationResponse;
import com.example.canvasia.dto.message.MessagePageResponse;
import com.example.canvasia.dto.message.MessageResponse;
import com.example.canvasia.dto.message.SendMessageRequest;
import com.example.canvasia.service.interfaces.MessageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ─── REST ─────────────────────────────────────────────────────────────────

    /** Get my conversation list */
    @GetMapping("/conversations")
    public List<ConversationResponse> getConversations(Authentication auth) {
        return messageService.getConversations(auth.getName());
    }

    /** Get or create a DM with another user */
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.OK)
    public ConversationResponse getOrCreateConversation(
            Authentication auth,
            @RequestParam String username
    ) {
        return messageService.getOrCreateConversation(auth.getName(), username);
    }

    /** Load paginated message history */
    @GetMapping("/conversations/{conversationId}")
    public MessagePageResponse getMessages(
            Authentication auth,
            @PathVariable UUID conversationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return messageService.getMessages(auth.getName(), conversationId, cursor, limit);
    }

    /** Mark conversation as read */
    @PostMapping("/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(Authentication auth, @PathVariable UUID conversationId) {
        messageService.markRead(auth.getName(), conversationId);
    }

    /** HTTP fallback send (useful for tests / non-WS clients) */
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendViaHttp(
            Authentication auth,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.sendMessage(auth.getName(), request);
    }

    // ─── WebSocket STOMP ──────────────────────────────────────────────────────

    /**
     * Client publishes to: /app/message.send
     * Server fans-out to:  /user/{sender}/queue/messages
     *                      /user/{recipient}/queue/messages
     *                      /user/{sender}/queue/conversations
     *                      /user/{recipient}/queue/conversations
     */
    @MessageMapping("message.send")
    public void handleWebSocketMessage(
            @Payload @Valid SendMessageRequest request,
            Principal principal
    ) {
        if (principal == null) return;
        messageService.sendMessage(principal.getName(), request);
    }
}

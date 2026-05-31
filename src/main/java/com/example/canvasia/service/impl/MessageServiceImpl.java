package com.example.canvasia.service.impl;

import com.example.canvasia.dto.message.ConversationResponse;
import com.example.canvasia.dto.message.MessagePageResponse;
import com.example.canvasia.dto.message.MessageResponse;
import com.example.canvasia.dto.message.SendMessageRequest;
import com.example.canvasia.entity.Conversation;
import com.example.canvasia.entity.Message;
import com.example.canvasia.entity.Profile;
import com.example.canvasia.entity.User;
import com.example.canvasia.repository.ConversationRepository;
import com.example.canvasia.repository.MessageRepository;
import com.example.canvasia.repository.ProfileRepository;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_PAGE_SIZE = 30;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Send ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse sendMessage(String senderUsername, SendMessageRequest request) {
        User sender = requireUser(senderUsername);

        Conversation conversation = resolveConversation(sender, request);

        Message message = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender(sender)
                        .content(request.content().trim())
                        .build()
        );

        conversation.applyNewMessage(message.getContent(), sender.getId());

        // Load both profiles before building responses
        Profile senderProfile   = profileRepository.findByUserId(sender.getId()).orElse(null);
        User    recipient        = conversation.getOtherParticipant(sender.getId());
        Profile recipientProfile = profileRepository.findByUserId(recipient.getId()).orElse(null);

        MessageResponse response = toMessageResponse(message, senderProfile);

        // From sender's view: "other" = recipient  → pass recipientProfile
        ConversationResponse convResponse = toConversationResponse(conversation, sender.getId(), recipientProfile);
        // From recipient's view: "other" = sender  → pass senderProfile
        ConversationResponse convForRecipient = toConversationResponse(conversation, recipient.getId(), senderProfile);

        String recipientUsername = recipient.getUsername();

        // Push new message to both sides (recipient only via WS; sender gets it in HTTP response)
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/messages", response);

        // Push updated conversation summary to both sides
        messagingTemplate.convertAndSendToUser(senderUsername,    "/queue/conversations", convResponse);
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/conversations", convForRecipient);

        return response;
    }

    // ─── History ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(String callerUsername, UUID conversationId,
                                           LocalDateTime cursor, int limit) {
        requireConversationAccess(callerUsername, conversationId);

        int pageSize = Math.min(limit, MAX_PAGE_SIZE);
        List<Message> messages;
        if (cursor == null) {
            messages = messageRepository.findFirstPageByConversation(
                    conversationId, PageRequest.of(0, pageSize + 1));
        } else {
            messages = messageRepository.findPageByConversationBefore(
                    conversationId, cursor, PageRequest.of(0, pageSize + 1));
        }

        boolean hasMore = messages.size() > pageSize;
        List<Message> page = hasMore ? messages.subList(0, pageSize) : messages;

        Set<UUID> senderIds = page.stream().map(m -> m.getSender().getId()).collect(Collectors.toSet());
        Map<UUID, Profile> profileMap = profileRepository.findByUserIdIn(senderIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));

        List<MessageResponse> items = page.stream()
                .map(m -> toMessageResponse(m, profileMap.get(m.getSender().getId())))
                .toList();

        LocalDateTime nextCursor = hasMore ? page.get(page.size() - 1).getCreatedAt() : null;

        return new MessagePageResponse(items, nextCursor, hasMore);
    }

    // ─── Conversations ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String callerUsername) {
        User caller = requireUser(callerUsername);
        List<Conversation> conversations = conversationRepository.findAllByParticipant(caller.getId());

        Set<UUID> otherIds = conversations.stream()
                .map(c -> c.getOtherParticipant(caller.getId()).getId())
                .collect(Collectors.toSet());
        Map<UUID, Profile> profileMap = profileRepository.findByUserIdIn(otherIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));

        return conversations.stream()
                .map(c -> toConversationResponse(c, caller.getId(), profileMap.get(c.getOtherParticipant(caller.getId()).getId())))
                .toList();
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(String callerUsername, String otherUsername) {
        User caller = requireUser(callerUsername);
        User other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Conversation conv = conversationRepository
                .findByParticipants(caller.getId(), other.getId())
                .orElseGet(() -> conversationRepository.save(Conversation.create(caller, other)));

        Profile otherProfile = profileRepository.findByUserId(other.getId()).orElse(null);
        return toConversationResponse(conv, caller.getId(), otherProfile);
    }

    // ─── Mark read ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markRead(String callerUsername, UUID conversationId) {
        User caller = requireUser(callerUsername);
        Conversation conv = requireConversationAccess(callerUsername, conversationId);

        messageRepository.markConversationReadFor(conversationId, caller.getId(), LocalDateTime.now());
        conv.markReadFor(caller.getId());

        // "other" participant from caller's view
        User other = conv.getOtherParticipant(caller.getId());
        Profile otherProfile = profileRepository.findByUserId(other.getId()).orElse(null);
        ConversationResponse updated = toConversationResponse(conv, caller.getId(), otherProfile);
        messagingTemplate.convertAndSendToUser(callerUsername, "/queue/conversations", updated);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private Conversation requireConversationAccess(String username, UUID conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        boolean isParticipant = conv.getParticipant1().getUsername().equals(username)
                || conv.getParticipant2().getUsername().equals(username);
        if (!isParticipant) {
            throw new IllegalArgumentException("Access denied");
        }
        return conv;
    }

    private Conversation resolveConversation(User sender, SendMessageRequest request) {
        if (request.conversationId() != null) {
            return requireConversationAccess(sender.getUsername(), request.conversationId());
        }
        if (request.recipientUsername() != null) {
            User recipient = userRepository.findByUsername(request.recipientUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            return conversationRepository
                    .findByParticipants(sender.getId(), recipient.getId())
                    .orElseGet(() -> conversationRepository.save(Conversation.create(sender, recipient)));
        }
        throw new IllegalArgumentException("Either conversationId or recipientUsername is required");
    }

    private MessageResponse toMessageResponse(Message message, Profile senderProfile) {
        String avatarUrl = senderProfile != null ? senderProfile.getAvatarUrl() : null;
        String displayName = senderProfile != null ? senderProfile.getDisplayName() : message.getSender().getUsername();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                displayName,
                avatarUrl,
                message.getContent(),
                message.getCreatedAt(),
                message.getReadAt()
        );
    }

    private ConversationResponse toConversationResponse(Conversation conv, UUID myUserId, Profile otherProfile) {
        User other = conv.getOtherParticipant(myUserId);
        String avatarUrl = otherProfile != null ? otherProfile.getAvatarUrl() : null;
        String displayName = otherProfile != null ? otherProfile.getDisplayName() : other.getUsername();
        return new ConversationResponse(
                conv.getId(),
                other.getId(),
                other.getUsername(),
                displayName,
                avatarUrl,
                conv.getLastMessagePreview(),
                conv.getLastSenderId(),
                conv.getLastMessageAt(),
                conv.getUnreadCountFor(myUserId)
        );
    }
}

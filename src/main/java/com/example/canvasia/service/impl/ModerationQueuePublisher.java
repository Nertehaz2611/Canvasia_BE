package com.example.canvasia.service.impl;

import com.example.canvasia.dto.moderation.ModerationQueueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModerationQueuePublisher {

    private static final Logger logger = LoggerFactory.getLogger(ModerationQueuePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String moderationQueue;

    public ModerationQueuePublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.moderation.queue:ai.moderation}") String moderationQueue
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper();
        this.moderationQueue = moderationQueue;
    }

    public void publish(String mediaId, String imageUrl) {
        publishUpsert(mediaId, imageUrl);
    }

    public void publishUpsert(String mediaId, String imageUrl) {
        if (mediaId == null || imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(new ModerationQueueMessage("UPSERT", mediaId, imageUrl));
            rabbitTemplate.convertAndSend(moderationQueue, payload);
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to serialize moderation message for media {}", mediaId, ex);
        } catch (AmqpException ex) {
            logger.warn("Failed to publish moderation message for media {}", mediaId, ex);
        }
    }

    public void publishDelete(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(new ModerationQueueMessage("DELETE", mediaId, null));
            rabbitTemplate.convertAndSend(moderationQueue, payload);
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to serialize moderation delete for media {}", mediaId, ex);
        } catch (AmqpException ex) {
            logger.warn("Failed to publish moderation delete for media {}", mediaId, ex);
        }
    }
}

package com.example.canvasia.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModerationQueueConfig {

    @Bean
    public Queue moderationQueue(@Value("${app.moderation.queue:ai.moderation}") String queueName) {
        return new Queue(queueName, true);
    }
}

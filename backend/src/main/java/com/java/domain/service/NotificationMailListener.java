package com.java.domain.service;

import com.java.domain.service.dto.UserOperationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMailListener {

    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;

    @RabbitListener(queues = "${app.messaging.user.queue}")
    public void handle(String payload) {
        log.info("=== NotificationMailListener RECEIVED ===");
        log.debug("Raw payload: {}", payload);

        try {
            UserOperationEvent event = objectMapper.readValue(payload, UserOperationEvent.class);

            log.info("eventType={}", event.eventType());
            log.info("userId={}", event.userId());
            log.info("username={}", event.username());

            userNotificationService.handle(event);

            log.info("=== NotificationMailListener DONE ===");
        } catch (JacksonException ex) {
            log.error("Failed to process RabbitMQ notification payload", ex);
            throw new IllegalStateException("Failed to process notification event", ex);
        }
    }
}
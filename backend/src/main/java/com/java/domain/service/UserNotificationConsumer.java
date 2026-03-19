package com.java.domain.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.java.domain.service.dto.UserOperationEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UserNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;

    @RabbitListener(queues = "${app.messaging.user.queue}")
    public void consume(String message) {
        try {
            UserOperationEvent event = objectMapper.readValue(message, UserOperationEvent.class);
            userNotificationService.handle(event);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Cannot process user event message", ex);
        }
    }
}

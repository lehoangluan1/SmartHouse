package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.NotificationMailProperties;
import com.java.domain.UserEventType;
import com.java.domain.service.dto.UserOperationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final NotificationMailProperties notificationMailProperties;

    public String resolve(UserOperationEvent event) {
        if (event == null) {
            log.warn("RecipientResolver: event is null");
            return null;
        }

        if (UserEventType.USER_PROVISIONED.equals(event.eventType())) {
            String adminRecipient = notificationMailProperties.adminTo();

            if (adminRecipient == null || adminRecipient.isBlank()) {
                log.warn("RecipientResolver: admin recipient is not configured for eventType={}", event.eventType());
                return null;
            }

            log.info("RecipientResolver: using admin recipient={} for eventType={}",
                    adminRecipient, event.eventType());
            return adminRecipient;
        }

        log.warn("RecipientResolver: no recipient rule configured for eventType={}", event.eventType());
        return null;
    }
}
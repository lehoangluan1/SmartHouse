package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.config.NotificationMailProperties;
import com.java.domain.service.dto.NotificationMessage;
import com.java.domain.service.dto.UserOperationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationMailProperties notificationMailProperties;
    private final NotificationRecipientResolver recipientResolver;
    private final UserNotificationComposerRegistry composerRegistry;
    private final NotificationMailSender notificationMailSender;

    public void handle(UserOperationEvent event) {
        log.info("UserNotificationService.handle START");

        if (event == null) {
            log.warn("Skip send mail because event is null");
            return;
        }

        log.info("Notification enabled = {}", notificationMailProperties.enabled());
        if (!Boolean.TRUE.equals(notificationMailProperties.enabled())) {
            log.warn("Skip send mail because notification mail is disabled");
            return;
        }

        log.info("Resolving recipient for eventType={}, userId={}", event.eventType(), event.userId());
        String recipient = recipientResolver.resolve(event);
        log.info("Resolved recipient = {}", recipient);

        if (recipient == null || recipient.isBlank()) {
            log.warn("Skip send mail because recipient is blank");
            return;
        }

        var composer = composerRegistry.get(event.eventType());
        log.info("Resolved composer = {}", composer.getClass().getSimpleName());

        NotificationMessage message = composer.compose(event, recipient);
        log.info("Composed message recipient = {}", message != null ? message.recipient() : null);
        log.info("Composed message subject = {}", message != null ? message.subject() : null);

        notificationMailSender.send(message);

        log.info("UserNotificationService.handle END");
    }
}
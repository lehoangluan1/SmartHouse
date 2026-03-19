package com.java.domain.service;

import org.springframework.stereotype.Component;
import com.java.domain.service.dto.NotificationMessage;
import com.java.domain.service.dto.UserOperationEvent;

@Component
public class DefaultUserNotificationComposer extends AbstractUserNotificationComposer {

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public NotificationMessage compose(UserOperationEvent event, String recipient) {
        String subject = "[SmartHouse] User notification";
        String body = """
                Event: %s
                Home ID: %s
                User ID: %s
                Username: %s
                Role In Home: %s
                Allow Profile Activation: %s
                Is Primary: %s
                Provider: %s
                Actor User ID: %s
                Occurred At: %s
                Metadata: %s
                """.formatted(
                safeObject(event.eventType()),
                safeObject(event.homeId()),
                safeObject(event.userId()),
                safe(event.username()),
                safe(event.roleInHome()),
                safeObject(event.allowProfileActivation()),
                safeObject(event.primary()),
                safe(event.provider()),
                safeObject(event.actorUserId()),
                safeObject(event.occurredAt()),
                safeObject(event.metadata())
        );

        return new NotificationMessage(recipient, subject, body);
    }
}
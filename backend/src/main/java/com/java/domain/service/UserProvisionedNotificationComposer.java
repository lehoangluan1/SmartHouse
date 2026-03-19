package com.java.domain.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.domain.UserEventType;
import com.java.domain.service.dto.NotificationMessage;
import com.java.domain.service.dto.UserOperationEvent;

@Component
public class UserProvisionedNotificationComposer extends AbstractUserNotificationComposer {

    @Override
    public boolean supports(String eventType) {
        return UserEventType.USER_PROVISIONED.equals(eventType);
    }

    @Override
    public NotificationMessage compose(UserOperationEvent event, String recipient) {
        Map<String, Object> metadata = event.metadata();

        String provider = safe(event.provider());
        String homeName = getString(metadata, "homeName");
        String homeAddress = getString(metadata, "homeAddress");
        String temporaryPassword = getString(metadata, "temporaryPassword");

        Object systemRole = getValue(metadata, "systemRole");
        Object homeRole = getValue(metadata, "homeRole");
        Object mustChangePassword = getValue(metadata, "mustChangePassword");
        Object createdNewHome = getValue(metadata, "createdNewHome");

        String subject = "[SmartHouse] Your account has been created";
        String body;

        if ("LOCAL".equalsIgnoreCase(provider)) {
            body = """
                    Hello %s,

                    Your SmartHouse account has been created successfully.

                    Login username/email: %s
                    Temporary password: %s
                    Must change password on next login: %s

                    Account information:
                    - System role: %s
                    - Home role: %s
                    - Home ID: %s
                    - Home name: %s
                    - Home address: %s
                    - Created new home: %s

                    Please sign in and change your temporary password immediately.
                    """.formatted(
                    username(event),
                    username(event),
                    safe(temporaryPassword),
                    safeObject(mustChangePassword),
                    safeObject(systemRole),
                    safeObject(homeRole),
                    safeObject(event.homeId()),
                    safe(homeName),
                    safe(homeAddress),
                    safeObject(createdNewHome)
            );
        } else {
            body = """
                    Hello %s,

                    Your SmartHouse account has been created successfully.

                    Sign-in provider: %s

                    Account information:
                    - System role: %s
                    - Home role: %s
                    - Home ID: %s
                    - Home name: %s
                    - Home address: %s

                    Please sign in using your linked provider.
                    """.formatted(
                    username(event),
                    safe(provider),
                    safeObject(systemRole),
                    safeObject(homeRole),
                    safeObject(event.homeId()),
                    safe(homeName),
                    safe(homeAddress)
            );
        }

        return new NotificationMessage(recipient, subject, body);
    }
}
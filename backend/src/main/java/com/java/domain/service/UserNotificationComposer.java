package com.java.domain.service;
import com.java.domain.service.dto.NotificationMessage;
import com.java.domain.service.dto.UserOperationEvent;

public interface UserNotificationComposer {

    boolean supports(String eventType);

    NotificationMessage compose(UserOperationEvent event, String recipient);
}
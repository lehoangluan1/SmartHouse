package com.java.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class UserNotificationComposerRegistry {

    private final List<UserNotificationComposer> composers;

    public UserNotificationComposerRegistry(List<UserNotificationComposer> composers) {
        this.composers = composers;
    }

    public UserNotificationComposer get(String eventType) {
        return composers.stream()
                .filter(composer -> composer.supports(eventType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No notification composer found for " + eventType));
    }
}
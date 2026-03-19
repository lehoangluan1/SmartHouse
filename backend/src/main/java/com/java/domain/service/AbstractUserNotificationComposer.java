package com.java.domain.service;

import java.util.Map;

import com.java.domain.service.dto.UserOperationEvent;

public abstract class AbstractUserNotificationComposer implements UserNotificationComposer {

    protected Object getValue(Map<String, Object> metadata, String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    protected String getString(Map<String, Object> metadata, String key) {
        Object value = getValue(metadata, key);
        return value != null ? String.valueOf(value) : null;
    }

    protected String safe(String value) {
        return value != null ? value : "";
    }

    protected String safeObject(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    protected String username(UserOperationEvent event) {
        return safe(event.username());
    }
}

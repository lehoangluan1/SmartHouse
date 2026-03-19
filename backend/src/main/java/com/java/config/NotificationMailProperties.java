package com.java.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.mail")
public record NotificationMailProperties(
        Boolean enabled,
        String from,
        String adminTo
) {
}
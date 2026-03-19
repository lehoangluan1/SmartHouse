package com.java.domain.service.dto;

public record NotificationMessage(
        String recipient,
        String subject,
        String body
) {
}

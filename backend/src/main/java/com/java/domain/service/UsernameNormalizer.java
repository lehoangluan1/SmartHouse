package com.java.domain.service;

import org.springframework.stereotype.Component;

@Component
public class UsernameNormalizer {

    public String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return normalized;
    }

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
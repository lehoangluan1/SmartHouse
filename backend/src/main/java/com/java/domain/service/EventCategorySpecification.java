package com.java.domain.service;

import com.java.controller.dto.AuditEventItem;
import java.util.Locale;

public class EventCategorySpecification {

    private final String category;

    public EventCategorySpecification(String category) {
        this.category = normalize(category);
    }

    public boolean isSatisfiedBy(AuditEventItem item) {
        if (category.isBlank() || "all".equals(category)) {
            return true;
        }

        return normalize(item.getCategory()).equals(category);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
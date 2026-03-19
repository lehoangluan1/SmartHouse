package com.java.domain.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AuditValueFormatter {

    public String normalizeTarget(String target) {
        if (target == null || target.isBlank()) {
            return "";
        }

        String normalized = target.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "MODE" -> "MODE";
            case "POWER" -> "POWER";
            case "LIGHT" -> "LIGHT";
            case "FAN" -> "FAN";
            case "PUMP" -> "PUMP";
            case "SPEED" -> "SPEED";
            case "BRIGHTNESS" -> "BRIGHTNESS";
            case "MOTION" -> "MOTION";
            case "ALERT" -> "ALERT";
            default -> normalized;
        };
    }

    public String normalizeStateValue(String target, Object value) {
        if (value == null) {
            return "-";
        }

        if (value instanceof Boolean boolValue) {
            return boolValue ? "ON" : "OFF";
        }

        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return "-";
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if ("true".equals(lower)) return "ON";
        if ("false".equals(lower)) return "OFF";

        if ("mode".equalsIgnoreCase(target)) {
            return lower;
        }

        return text;
    }

    public String displayState(String value) {
        return isBlank(value) ? "-" : value;
    }

    public String displayRaw(Object value) {
        if (value == null) return "-";
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "-" : text;
    }

    public boolean isMissingState(String value) {
        return value == null || value.isBlank() || "-".equals(value);
    }

    public boolean hasStateValues(String fromState, String toState) {
        return !isMissingState(fromState) || !isMissingState(toState);
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean isBlankTarget(String target) {
        return target == null || target.isBlank();
    }

    public boolean isOnOffValue(String value) {
        return "ON".equalsIgnoreCase(value) || "OFF".equalsIgnoreCase(value);
    }

    public String safe(String value) {
        return value == null ? "" : value;
    }

    public String nonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
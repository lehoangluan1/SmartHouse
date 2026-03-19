package com.java.domain.service;

import com.java.domain.service.dto.AuditResolvedState;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AuditEventDetailsRefiner {

    public String refine(AuditResolvedState state) {
        if (state == null) {
            return "-";
        }

        if (sameState(state.fromState(), state.toState())) {
            if ("MODE".equalsIgnoreCase(safe(state.type()))) {
                return "Mode synced";
            }
            if ("POWER".equalsIgnoreCase(safe(state.type()))) {
                return "Power synced";
            }
            if ("SPEED".equalsIgnoreCase(safe(state.type()))) {
                return "Fan speed synced";
            }
            if ("BRIGHTNESS".equalsIgnoreCase(safe(state.type()))) {
                return "Brightness synced";
            }
        }

        return nonBlank(state.details(), "-");
    }

    private boolean sameState(String left, String right) {
        String a = normalizeComparableState(left);
        String b = normalizeComparableState(right);
        return !a.isBlank() && a.equals(b);
    }

    private String normalizeComparableState(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if ("-".equals(normalized)) return "";
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "-";
    }
}
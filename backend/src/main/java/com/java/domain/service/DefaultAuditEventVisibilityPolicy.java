package com.java.domain.service;
import com.java.controller.dto.AuditEventItem;
import com.java.domain.provider.AuditEventVisibilityPolicy;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DefaultAuditEventVisibilityPolicy implements AuditEventVisibilityPolicy {

    private static final String ACTION_MANUAL_HOLD_STARTED = "MANUAL_HOLD_STARTED";
    private static final String ACTION_MANUAL_HOLD_RESTORED = "MANUAL_HOLD_RESTORED";

    @Override
    public boolean shouldDisplay(AuditEventItem item) {
        if (item == null) {
            return false;
        }

        if (ACTION_MANUAL_HOLD_STARTED.equals(item.getType())
                || ACTION_MANUAL_HOLD_RESTORED.equals(item.getType())) {
            return true;
        }

        return !sameState(item.getFromState(), item.getToState());
    }

    private boolean sameState(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return !a.isBlank() && a.equals(b);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if ("-".equals(normalized)) {
            return "";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}

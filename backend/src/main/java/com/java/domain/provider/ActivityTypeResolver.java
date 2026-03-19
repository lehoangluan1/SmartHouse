package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityTypeResolver {

    private final AuditValueFormatter auditValueFormatter;

    public String resolve(ActivityAuditResolutionContext context, String normalizedTarget) {
        if (!auditValueFormatter.isBlankTarget(normalizedTarget)) {
            return normalizedTarget;
        }

        String action = auditValueFormatter.safe(context.action()).trim().toUpperCase();
        if (action.isBlank()) {
            return "SYSTEM_EVENT";
        }

        return switch (action) {
            case "MANUAL_HOLD_STARTED" -> "MANUAL_HOLD_STARTED";
            case "MANUAL_HOLD_RESTORE", "MANUAL_HOLD_RESTORED" -> "MANUAL_HOLD_RESTORED";
            case "MANUAL_HOLD_CLEARED" -> "MANUAL_HOLD_CLEARED";
            case "INIT_DEVICE" -> "INIT_DEVICE";
            case "UPSERT_CONFIG" -> "UPSERT_CONFIG";
            case "UPDATE_CONFIG" -> "UPDATE_CONFIG";
            case "ACTIVATE_CONFIG" -> "ACTIVATE_CONFIG";
            case "DELETE_CONFIG" -> "DELETE_CONFIG";
            case "ALERT_ACTIVE", "ALERT_ACK", "ALERT_RESOLVED" -> action;
            default -> action;
        };
    }
}
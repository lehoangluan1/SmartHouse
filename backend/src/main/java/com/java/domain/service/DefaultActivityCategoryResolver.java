package com.java.domain.service;

import com.java.domain.provider.ActivityCategoryResolver;
import com.java.persistence.entity.ActivityLogEntity;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultActivityCategoryResolver implements ActivityCategoryResolver {

    public static final String CATEGORY_ALERTS = "alerts";
    public static final String CATEGORY_DEVICE = "device";
    public static final String CATEGORY_SYSTEM = "system";

    private static final Set<String> ALERT_ACTIONS = Set.of(
            "ALERT_ACTIVE",
            "ALERT_ACK",
            "ALERT_RESOLVED"
    );

    private static final Set<String> SYSTEM_ACTIONS = Set.of(
            "INIT_DEVICE",
            "UPSERT_CONFIG",
            "UPDATE_CONFIG",
            "ACTIVATE_CONFIG",
            "DELETE_CONFIG",
            "MANUAL_HOLD_STARTED",
            "MANUAL_HOLD_RESTORED",
            "MANUAL_HOLD_RESTORE",
            "MANUAL_HOLD_CLEARED"
    );

    private static final Set<String> DEVICE_ACTIONS = Set.of(
            "MANUAL_CONTROL",
            "AUTO_CONTROL",
            "INGEST_TELEMETRY",
            "INGEST_TELEMETRY_NO_STATE_CHANGE"
    );

    private final AuditValueFormatter auditValueFormatter;

    @Override
    public String resolve(ActivityLogEntity entity) {
        if (entity == null) {
            return CATEGORY_SYSTEM;
        }

        String action = auditValueFormatter.safe(entity.getAction()).trim().toUpperCase();
        String method = auditValueFormatter.safe(entity.getMethod()).trim().toUpperCase();

        if (ALERT_ACTIONS.contains(action)) {
            return CATEGORY_ALERTS;
        }

        if (SYSTEM_ACTIONS.contains(action)) {
            return CATEGORY_SYSTEM;
        }

        if (DEVICE_ACTIONS.contains(action)) {
            return CATEGORY_DEVICE;
        }

        if ("OBSERVER".equals(method) && action.startsWith("ALERT_")) {
            return CATEGORY_ALERTS;
        }

        if ("SYSTEM".equals(method)) {
            return CATEGORY_SYSTEM;
        }

        if ("APP".equals(method) || "DEVICE".equals(method)) {
            return CATEGORY_DEVICE;
        }

        return entity.getDevice() != null ? CATEGORY_DEVICE : CATEGORY_SYSTEM;
    }
}
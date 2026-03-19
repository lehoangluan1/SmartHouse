package com.java.domain.service;

import com.java.domain.provider.ActivityStatusResolver;
import com.java.persistence.entity.ActivityLogEntity;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultActivityStatusResolver implements ActivityStatusResolver {

    private static final String ACTION_MANUAL_HOLD_RESTORED = "MANUAL_HOLD_RESTORED";
    private static final String ACTION_MANUAL_HOLD_RESTORE = "MANUAL_HOLD_RESTORE";

    private final AuditValueFormatter auditValueFormatter;

    @Override
    public String resolve(ActivityLogEntity entity) {
        String action = auditValueFormatter.safe(entity.getAction()).toUpperCase(Locale.ROOT);
        String text = (
                auditValueFormatter.safe(entity.getAction()) + " " +
                auditValueFormatter.safe(entity.getMethod()) + " " +
                auditValueFormatter.safe(auditValueFormatter.displayRaw(entity.getDetail()))
        ).toLowerCase(Locale.ROOT);

        if (ACTION_MANUAL_HOLD_RESTORED.equals(action) || ACTION_MANUAL_HOLD_RESTORE.equals(action)) {
            return "RESOLVED";
        }
        if (text.contains("resolved") || text.contains("resolve")) return "RESOLVED";
        if (text.contains("ack")) return "ACKNOWLEDGED";
        if (text.contains("failed") || text.contains("fail") || text.contains("error")) return "FAILED";
        if (text.contains("active") || text.contains("open")) return "ACTIVE";
        if (text.contains("success") || text.contains("updated") || text.contains("changed")) return "SUCCESS";
        return "LOGGED";
    }
}
package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManualHoldStartedResolutionStrategy implements ActivityEventResolutionStrategy {

    private static final String ACTION_MANUAL_HOLD_STARTED = "MANUAL_HOLD_STARTED";

    private final AuditValueFormatter auditValueFormatter;

    @Override
    public boolean supports(ActivityAuditResolutionContext context) {
        return ACTION_MANUAL_HOLD_STARTED.equals(context.action());
    }

    @Override
    public AuditResolvedState resolve(ActivityAuditResolutionContext context) {
        return AuditResolvedState.builder()
                .type(ACTION_MANUAL_HOLD_STARTED)
                .fromState("-")
                .toState("-")
                .details(auditValueFormatter.nonBlank(
                        context.parsedDetail().description(),
                        "Manual hold started"
                ))
                .build();
    }
}
package com.java.domain.provider;

import com.java.domain.service.AuditStateResolver;
import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManualHoldRestoredResolutionStrategy implements ActivityEventResolutionStrategy {

    private static final String ACTION_MANUAL_HOLD_RESTORED = "MANUAL_HOLD_RESTORED";
    private static final String ACTION_MANUAL_HOLD_RESTORE = "MANUAL_HOLD_RESTORE";

    private final AuditStateResolver auditStateResolver;
    private final AuditValueFormatter auditValueFormatter;
    private final CapabilityCodeResolver capabilityCodeResolver;

    @Override
    public boolean supports(ActivityAuditResolutionContext context) {
        return ACTION_MANUAL_HOLD_RESTORED.equals(context.action())
                || ACTION_MANUAL_HOLD_RESTORE.equals(context.action());
    }

    @Override
    public AuditResolvedState resolve(ActivityAuditResolutionContext context) {
        String type = ACTION_MANUAL_HOLD_RESTORED;
        String capabilityCode = capabilityCodeResolver.resolve(
                context.parsedDetail().target(),
                type
        );

        return auditStateResolver.resolveTransitionAroundEvent(
                context.deviceId(),
                capabilityCode,
                context.entity().getCreatedAt(),
                type,
                "-",
                auditValueFormatter.displayState(
                        auditValueFormatter.normalizeStateValue("MODE", context.parsedDetail().value())
                ),
                auditValueFormatter.nonBlank(
                        context.parsedDetail().description(),
                        "Manual hold restored"
                )
        );
    }
}
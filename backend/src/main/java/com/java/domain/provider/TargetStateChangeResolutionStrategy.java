package com.java.domain.provider;

import com.java.domain.service.AuditStateResolver;
import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;
import com.java.domain.service.dto.ExtractedActivityState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TargetStateChangeResolutionStrategy implements ActivityEventResolutionStrategy {

    private final AuditStateResolver auditStateResolver;
    private final AuditValueFormatter auditValueFormatter;
    private final ActivityStateExtractor activityStateExtractor;
    private final ActivityTypeResolver activityTypeResolver;
    private final ActivityDetailMessageResolver detailMessageResolver;
    private final CapabilityCodeResolver capabilityCodeResolver;

    @Override
    public boolean supports(ActivityAuditResolutionContext context) {
        return !auditValueFormatter.isBlankTarget(context.parsedDetail().target());
    }

    @Override
    public AuditResolvedState resolve(ActivityAuditResolutionContext context) {
        String normalizedTarget = auditValueFormatter.normalizeTarget(context.parsedDetail().target());
        ExtractedActivityState extracted = activityStateExtractor.extract(context, normalizedTarget);
        String resolvedType = activityTypeResolver.resolve(context, normalizedTarget);
        String capabilityCode = capabilityCodeResolver.resolve(normalizedTarget, resolvedType);

        return auditStateResolver.resolve(
                context.deviceId(),
                capabilityCode,
                context.entity().getCreatedAt(),
                resolvedType,
                extracted.fromState(),
                extracted.toState(),
                detailMessageResolver.resolve(context, normalizedTarget, extracted.details())
        );
    }
}
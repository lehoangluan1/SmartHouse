package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditParsedDetail;
import com.java.domain.service.dto.AuditResolvedState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityEventStateResolver {

    private final ActivityTypeResolver activityTypeResolver;
    private final AuditValueFormatter auditValueFormatter;

    public AuditResolvedState resolve(ActivityAuditResolutionContext context) {
        AuditParsedDetail parsedDetail = context.parsedDetail();

        String normalizedTarget = auditValueFormatter.normalizeTarget(parsedDetail.target());

        Object rawFromState = parsedDetail.effectiveFromState();
        Object rawToState = parsedDetail.effectiveToState();

        String resolvedType = activityTypeResolver.resolve(context, normalizedTarget);

        String fromState = auditValueFormatter.displayState(
                auditValueFormatter.normalizeStateValue(normalizedTarget, rawFromState)
        );

        String toState = auditValueFormatter.displayState(
                auditValueFormatter.normalizeStateValue(normalizedTarget, rawToState)
        );

        return new AuditResolvedState(
                resolvedType,
                fromState,
                toState,
                parsedDetail.description()
        );
    }
}
package com.java.domain.service;

import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;
import org.springframework.stereotype.Component;

@Component
public class ActivityFallbackStateFactory {

    private final AuditValueFormatter auditValueFormatter;

    public ActivityFallbackStateFactory(AuditValueFormatter auditValueFormatter) {
        this.auditValueFormatter = auditValueFormatter;
    }

    public AuditResolvedState create(ActivityAuditResolutionContext context) {
        String fallbackType = auditValueFormatter.nonBlank(
                context.entity().getAction(),
                "SYSTEM_EVENT"
        );

        String fromState = auditValueFormatter.displayState(
                auditValueFormatter.normalizeStateValue(
                        null,
                        context.entity().getOldValue()
                )
        );

        String toState = auditValueFormatter.displayState(
                auditValueFormatter.normalizeStateValue(
                        null,
                        context.entity().getNewValue()
                )
        );

        if (auditValueFormatter.isMissingState(fromState)) {
            fromState = auditValueFormatter.displayState(
                    auditValueFormatter.normalizeStateValue(
                            null,
                            context.parsedDetail().effectiveFromState()
                    )
            );
        }

        if (auditValueFormatter.isMissingState(toState)) {
            toState = auditValueFormatter.displayState(
                    auditValueFormatter.normalizeStateValue(
                            null,
                            context.parsedDetail().effectiveToState()
                    )
            );
        }

        String details = auditValueFormatter.nonBlank(
                context.parsedDetail().description(),
                auditValueFormatter.displayRaw(context.entity().getDetail()),
                fallbackType
        );

        return AuditResolvedState.builder()
                .type(fallbackType)
                .fromState(fromState)
                .toState(toState)
                .details(details)
                .build();
    }
}
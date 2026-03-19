package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditParsedDetail;
import com.java.domain.service.dto.ExtractedActivityState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityStateExtractor {

    private final AuditValueFormatter auditValueFormatter;

    public ExtractedActivityState extract(
            ActivityAuditResolutionContext context,
            String normalizedTarget
    ) {
        AuditParsedDetail parsedDetail = context.parsedDetail();

        Object rawFromState = parsedDetail.effectiveFromState();
        Object rawToState = parsedDetail.effectiveToState();

        String fromState = auditValueFormatter.normalizeStateValue(normalizedTarget, rawFromState);
        String toState = auditValueFormatter.normalizeStateValue(normalizedTarget, rawToState);

        return new ExtractedActivityState(
                auditValueFormatter.displayState(fromState),
                auditValueFormatter.displayState(toState),
                parsedDetail.description()
        );
    }
}
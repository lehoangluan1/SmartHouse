package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityDetailMessageResolver {

    private final AuditValueFormatter auditValueFormatter;

    public String resolve(
            ActivityAuditResolutionContext context,
            String normalizedTarget,
            String extractedDetails
    ) {
        if (!auditValueFormatter.isBlank(extractedDetails) && !"-".equals(extractedDetails)) {
            return extractedDetails;
        }

        if (!auditValueFormatter.isBlankTarget(normalizedTarget)) {
            if ("MODE".equals(normalizedTarget)) {
                return "Mode changed";
            }
            return normalizedTarget + " changed";
        }

        String action = auditValueFormatter.safe(context.action()).trim().toUpperCase();
        if (!action.isBlank()) {
            return action.replace('_', ' ');
        }

        return "Device changed";
    }
}
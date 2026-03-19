package com.java.domain.provider;

import com.java.domain.service.AuditValueFormatter;
import com.java.domain.service.dto.CommandAuditContext;
import com.java.persistence.entity.ControlCommandEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultCommandAuditProvider implements CommandAuditProvider {

    private static final String CAPABILITY_MODE = "MODE";
    private static final String CAPABILITY_POWER = "POWER";

    private final AuditValueFormatter auditValueFormatter;

    @Override
    public CommandAuditContext provide(ControlCommandEntity entity) {
        String normalizedTarget = auditValueFormatter.normalizeTarget(entity.getTarget());
        Object rawValue = extractRawValue(entity);
        String toState = auditValueFormatter.normalizeStateValue(entity.getTarget(), rawValue);

        return CommandAuditContext.builder()
                .normalizedTarget(normalizedTarget)
                .capabilityCode(resolveCapabilityCode(normalizedTarget))
                .toState(toState)
                .fallbackDetails(buildFallbackDetails(normalizedTarget, toState))
                .build();
    }

    private Object extractRawValue(ControlCommandEntity entity) {
        if (entity.getValueBoolean() != null) return entity.getValueBoolean();
        if (entity.getValueNumber() != null) return entity.getValueNumber();
        if (entity.getValueText() != null) return entity.getValueText();
        return null;
    }

    private String resolveCapabilityCode(String normalizedTarget) {
        if (CAPABILITY_MODE.equals(normalizedTarget)) return CAPABILITY_MODE;
        if (CAPABILITY_POWER.equals(normalizedTarget)) return CAPABILITY_POWER;
        return null;
    }

    private String buildFallbackDetails(String normalizedTarget, String toState) {
        if (CAPABILITY_MODE.equals(normalizedTarget)) {
            return "Mode command";
        }
        if (auditValueFormatter.isOnOffValue(toState)) {
            return normalizedTarget + " command";
        }
        return "Command " + normalizedTarget;
    }
}
package com.java.domain.provider;

import com.java.domain.service.ActivityFallbackStateFactory;
import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultActivityResolutionStrategy implements ActivityEventResolutionStrategy {

    private final ActivityFallbackStateFactory fallbackStateFactory;

    @Override
    public boolean supports(ActivityAuditResolutionContext context) {
        return true;
    }

    @Override
    public AuditResolvedState resolve(ActivityAuditResolutionContext context) {
        return fallbackStateFactory.create(context);
    }
}
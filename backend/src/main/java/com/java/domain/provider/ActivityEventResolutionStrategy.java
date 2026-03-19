package com.java.domain.provider;

import com.java.domain.service.dto.ActivityAuditResolutionContext;
import com.java.domain.service.dto.AuditResolvedState;

public interface ActivityEventResolutionStrategy {
    boolean supports(ActivityAuditResolutionContext context);
    AuditResolvedState resolve(ActivityAuditResolutionContext context);
}
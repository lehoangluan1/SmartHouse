package com.java.domain.service.dto;

import lombok.Builder;

@Builder
public record CommandAuditContext(
        String normalizedTarget,
        String capabilityCode,
        String toState,
        String fallbackDetails
) {
}
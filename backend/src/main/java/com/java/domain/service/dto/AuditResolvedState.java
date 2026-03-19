package com.java.domain.service.dto;

import lombok.Builder;

@Builder
public record AuditResolvedState(
        String type,
        String fromState,
        String toState,
        String details
) {
}
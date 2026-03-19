package com.java.domain.service.dto;

import lombok.Builder;

@Builder
public record AuditActivityStateView(
        String type,
        String fromState,
        String toState,
        String details
) {
}
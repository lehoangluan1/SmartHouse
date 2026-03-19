package com.java.domain.service.dto;

import lombok.Builder;

@Builder
public record AuditStatePair(
        String fromState,
        String toState
) {
}
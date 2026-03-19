package com.java.domain.service.dto;
import lombok.Builder;

@Builder
public record ExtractedActivityState(
        String fromState,
        String toState,
        String details
) {
}
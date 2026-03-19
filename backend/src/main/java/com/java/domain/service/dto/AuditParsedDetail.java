package com.java.domain.service.dto;

import lombok.Builder;

@Builder
public record AuditParsedDetail(
        String target,
        Object value,
        Object previousValue,
        Object fromState,
        Object toState,
        String description
) {
    public static AuditParsedDetail empty() {
        return AuditParsedDetail.builder().build();
    }

    public static AuditParsedDetail withDescription(String description) {
        return AuditParsedDetail.builder()
                .description(description)
                .build();
    }

    public Object effectiveFromState() {
        return fromState != null ? fromState : previousValue;
    }

    public Object effectiveToState() {
        return toState != null ? toState : value;
    }
}
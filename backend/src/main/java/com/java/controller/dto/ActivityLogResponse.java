package com.java.controller.dto;

import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record ActivityLogResponse(
        Long id,
        Long homeId,
        Long deviceId,
        String deviceName,
        Long userId,
        String username,
        String action,
        String method,
        String oldValue,
        String newValue,
        String detail,
        OffsetDateTime createdAt
) {
}
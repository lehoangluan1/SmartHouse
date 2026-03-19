package com.java.controller.dto;

import java.time.OffsetDateTime;

public record ConfigResponse(
        Long id,
        Long homeId,
        String name,
        Long createdBy,
        Boolean active,
        ConfigThresholdsDto thresholds,
        ConfigMonitoringSlotsDto monitoringSlots,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
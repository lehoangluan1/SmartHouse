package com.java.controller.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleUpsertRequest(
        Long id,
        @NotNull Long deviceId,
        String capabilityCode,
        String value,
        String mode,
        @NotNull LocalTime startTime,
        LocalTime endTime,
        @Min(0) @Max(127) Integer daysMask,
        Boolean enabled,
        Integer priority
) {}
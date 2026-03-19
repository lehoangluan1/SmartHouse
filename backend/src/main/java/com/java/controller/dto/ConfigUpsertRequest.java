package com.java.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigUpsertRequest(

        @NotBlank(message = "Name must not be blank")
        String name,

        @Valid
        @NotNull(message = "Thresholds configuration is required")
        ConfigThresholdsDto thresholds,

        @Valid
        @NotNull(message = "Monitoring slots configuration is required")
        ConfigMonitoringSlotsDto monitoringSlots

) {
}
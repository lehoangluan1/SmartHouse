package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelemetryIngestRequest(
        @NotBlank(message = "deviceKey cannot be empty") String deviceKey,
        @NotBlank(message = "sensorType cannot be empty") String sensorType,
        @NotNull(message = "value cannot be null") Object value
) {}

package com.java.controller.dto;

import com.java.domain.AlertType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertOpenRequest(
        Long deviceId,
        Long sensorId,
        @NotNull(message = "type cannot be empty") AlertType type,
        @NotBlank(message = "message cannot be empty") String message
) {
}
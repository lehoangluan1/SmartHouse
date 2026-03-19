package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ControlRequest(
        @NotBlank(message = "target cannot be empty") String target,
        @NotBlank(message = "value cannot be empty") String value,
        @NotNull(message = "actorId cannot be null") Long actorId,
        String actorName,
        String method
) {}

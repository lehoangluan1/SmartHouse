package com.java.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DeviceCreateRequest(
        @NotBlank(message = "Device name cannot be empty")
        String name,

        @NotBlank(message = "Subtype cannot be empty")
        String subtype,

        String roomName,

        @NotBlank(message = "Device key cannot be empty")
        String deviceKey
) {}
package com.java.domain.service.dto;

import java.time.OffsetDateTime;

import com.java.domain.SystemMode;

public record ManualHoldState(
        Long deviceId,
        Long homeId,
        SystemMode previousMode,
        OffsetDateTime holdUntil,
        boolean active
) {
}
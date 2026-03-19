package com.java.domain.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.java.persistence.entity.ConfigEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OfflineDetector {

    private final DeviceConfigService deviceConfigService;

    public boolean isOffline(Long deviceId, OffsetDateTime lastSeenAt, OffsetDateTime now) {
        if (lastSeenAt == null) {
            return false;
        }

        ConfigEntity cfg = null;
        try {
            cfg = deviceConfigService.getActiveConfig(deviceId);
        } catch (Exception ignored) {
        }

        int mMinutes = Optional.ofNullable(cfg)
                        .map(c -> c.getMMinutes())
                        .filter(m -> m > 0)
                        .orElse(5);

        return lastSeenAt.isBefore(now.minus(mMinutes, ChronoUnit.MINUTES));
    }
}
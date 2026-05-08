package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.java.persistence.repo.DeviceStateHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationCooldownService {

    private final DeviceStateHistoryRepository deviceStateHistoryRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;

    public boolean isCoolingDown(Long deviceId, String target, String value, Integer kMinutes) {
        if (deviceId == null || target == null || target.isBlank() || kMinutes == null || kMinutes <= 0) {
            return false;
        }

        String normalizedTarget = normalizeTargetSafe(target);
        String normalizedValue = normalizeValueSafe(value);

        return deviceStateHistoryRepository
                .findLastAutomationAt(deviceId, normalizedTarget, normalizedValue)
                .map(lastAt -> lastAt.isAfter(OffsetDateTime.now().minusMinutes(kMinutes)))
                .orElse(false);
    }

    private String normalizeTargetSafe(String target) {
        try {
            return deviceTargetPolicy.normalizeTarget(target);
        } catch (RuntimeException e) {
            return target.trim().toUpperCase(Locale.ROOT);
        }
    }

    private String normalizeValueSafe(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
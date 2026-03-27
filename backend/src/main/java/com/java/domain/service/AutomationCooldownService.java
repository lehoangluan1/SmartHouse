package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.java.persistence.repo.DeviceStateHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationCooldownService {

    private final DeviceStateHistoryRepository deviceStateHistoryRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;

    public boolean isCoolingDown(Long deviceId, String target, Integer kMinutes) {
        if (deviceId == null || target == null || target.isBlank() || kMinutes == null || kMinutes <= 0) {
            return false;
        }

        String normalizedTarget = normalizeTargetSafe(target);

        return deviceStateHistoryRepository.findLastAutomationAt(deviceId, normalizedTarget)
                .map(lastAt -> lastAt.isAfter(OffsetDateTime.now().minusMinutes(kMinutes)))
                .orElse(false);
    }

    private String normalizeTargetSafe(String target) {
        try {
            return deviceTargetPolicy.normalizeTarget(target);
        } catch (RuntimeException e) {
            return target.trim().toUpperCase();
        }
    }
}
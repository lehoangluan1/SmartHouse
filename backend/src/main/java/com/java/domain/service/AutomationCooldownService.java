package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;
import com.java.persistence.repo.ConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutomationCooldownService {

    private final ConfigRepository configRepository;
    private final DeviceTargetPolicy deviceTargetPolicy;

    public boolean isCoolingDown(Long homeId, Map<String, DeviceRuntimeStateEntity> stateMap, String target) {
        if (homeId == null || target == null || target.isBlank()) {
            return false;
        }

        ConfigEntity cfg = configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId).orElse(null);
        if (cfg == null || cfg.getKMinutes() == null || cfg.getKMinutes() <= 0) {
            return false;
        }

        DeviceRuntimeStateEntity state = resolveState(stateMap, target);
        if (state == null || state.getUpdatedAt() == null) {
            return false;
        }

        return state.getUpdatedAt().isAfter(OffsetDateTime.now().minusMinutes(cfg.getKMinutes()));
    }

    private DeviceRuntimeStateEntity resolveState(
            Map<String, DeviceRuntimeStateEntity> stateMap,
            String target
    ) {
        if (stateMap == null || stateMap.isEmpty()) {
            return null;
        }

        try {
            String normalizedTarget = deviceTargetPolicy.normalizeTarget(target);
            DeviceRuntimeStateEntity normalizedState = stateMap.get(normalizedTarget);
            if (normalizedState != null) {
                return normalizedState;
            }
        } catch (RuntimeException ignored) {
            // Fall back to raw lookup to remain tolerant of legacy aliases.
        }

        DeviceRuntimeStateEntity rawState = stateMap.get(target);
        if (rawState != null) {
            return rawState;
        }

        rawState = stateMap.get(target.toUpperCase());
        if (rawState != null) {
            return rawState;
        }

        return stateMap.get(target.toLowerCase());
    }
}

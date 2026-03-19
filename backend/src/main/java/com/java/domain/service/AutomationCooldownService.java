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

    public boolean isCoolingDown(Long homeId, Map<String, DeviceRuntimeStateEntity> stateMap, String target) {
        if (homeId == null || target == null || target.isBlank()) {
            return false;
        }

        ConfigEntity cfg = configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId).orElse(null);
        if (cfg == null || cfg.getKMinutes() == null || cfg.getKMinutes() <= 0) {
            return false;
        }

        DeviceRuntimeStateEntity state = stateMap.get(target.toLowerCase());
        if (state == null || state.getUpdatedAt() == null) {
            state = stateMap.get(target.toUpperCase());
        }
        if (state == null || state.getUpdatedAt() == null) {
            return false;
        }

        return state.getUpdatedAt().isAfter(OffsetDateTime.now().minusMinutes(cfg.getKMinutes()));
    }
}
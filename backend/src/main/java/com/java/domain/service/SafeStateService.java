package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.ConfigEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SafeStateService {

    private static final String SOURCE_SAFE_STATE = "safe_state";

    private final DeviceRuntimeStateService deviceRuntimeStateService;

    @Transactional
    public void applyForInvalidTelemetry(Long sourceDeviceId, ConfigEntity config, String reason) {
        if (config == null) {
            return;
        }

        applyFanSafeState(config);
        applyLightSafeState(config);
    }

    private void applyFanSafeState(ConfigEntity config) {
        if (config.getMonitoringFanDevice() == null) {
            return;
        }

        Long fanId = config.getMonitoringFanDevice().getId();

        applyIfSupported(fanId, "POWER", false);
        applyIfSupported(fanId, "SPEED", 0);
    }

    private void applyLightSafeState(ConfigEntity config) {
        if (config.getMonitoringLightDevice() == null) {
            return;
        }

        Long lightId = config.getMonitoringLightDevice().getId();

        applyIfSupported(lightId, "POWER", false);
        applyIfSupported(lightId, "BRIGHTNESS", 0);
    }

    private void applyIfSupported(Long deviceId, String capabilityCode, Object value) {
        try {
            deviceRuntimeStateService.applyState(
                    deviceId,
                    capabilityCode,
                    value,
                    SOURCE_SAFE_STATE,
                    null,
                    null
            );
        } catch (Exception ignored) {
        }
    }
}
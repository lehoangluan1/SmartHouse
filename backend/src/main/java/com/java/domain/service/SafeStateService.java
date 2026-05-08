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
    private final DeviceControlService deviceControlService;

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

        var fan = config.getMonitoringFanDevice();

        applyIfSupported(fan, "POWER", false);
        applyIfSupported(fan, "SPEED", 0);
    }

    private void applyLightSafeState(ConfigEntity config) {
        if (config.getMonitoringLightDevice() == null) {
            return;
        }

        var light = config.getMonitoringLightDevice();

        applyIfSupported(light, "POWER", false);
        applyIfSupported(light, "BRIGHTNESS", 0);
    }

    private void applyIfSupported(com.java.persistence.entity.DeviceEntity device, String capabilityCode, Object value) {
        try {
            if (device == null || device.getId() == null
                    || !deviceRuntimeStateService.hasChanged(device.getId(), capabilityCode, value)) {
                return;
            }

            deviceControlService.controlDevice(
                    device,
                    capabilityCode,
                    value,
                    "system",
                    null,
                    SOURCE_SAFE_STATE
            );
        } catch (Exception ignored) {
        }
    }
}

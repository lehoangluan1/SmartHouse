package com.java.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvalidTelemetryHandler {

    private final DeviceConfigService deviceConfigService;
    private final SafeStateService safeStateService;
    private final AlertLifecycleService alertLifecycleService;
    private final TelemetryErrorAuditService telemetryErrorAuditService;

    @Transactional
    public void handle(DeviceEntity sourceDevice, String sensorType, Object rawValue, String reason) {
        telemetryErrorAuditService.logInvalidPayload(sourceDevice, sensorType, rawValue, reason);

        if (sourceDevice == null) {
            return;
        }

        alertLifecycleService.upsertActiveAlert(
                sourceDevice.getId(),
                null,
                com.java.domain.AlertType.SENSOR_ERROR,
                "Invalid telemetry for " + sensorType + ": " + reason
        );

        ConfigEntity config = deviceConfigService.getActiveConfig(sourceDevice.getId());
        safeStateService.applyForInvalidTelemetry(sourceDevice.getId(), config, reason);
    }
}
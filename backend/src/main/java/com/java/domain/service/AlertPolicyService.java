package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.InvalidTelemetryException;
import com.java.domain.AlertType;
import com.java.persistence.entity.SensorDataEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertPolicyService {

    private final TelemetryValidationService telemetryValidationService;
    private final CriticalTemperatureService criticalTemperatureService;
    private final HighTemperatureDurationService highTemperatureDurationService;
    private final OfflineDetector offlineDetector;
    private final AlertLifecycleService alertLifecycleService;

    public boolean isValidTelemetry(SensorDataEntity data) {
        try {
            telemetryValidationService.validateOrThrow(data);
            return true;
        } catch (InvalidTelemetryException ex) {
            return false;
        }
    }

    @Transactional
    public void raiseMotionDetected(Long deviceId, String message) {
        alertLifecycleService.upsertActiveAlert(deviceId, null, AlertType.MOTION_DETECTED, message);
    }

    @Transactional
    public void resolveMotionDetected(Long deviceId) {
        alertLifecycleService.resolveIfExists(deviceId, null, AlertType.MOTION_DETECTED);
    }

    @Transactional
    public void raiseSensorError(Long deviceId, String message) {
        alertLifecycleService.upsertActiveAlert(deviceId, null, AlertType.SENSOR_ERROR, message);
    }

    @Transactional
    public void handleCriticalTemp(Long deviceId, SensorDataEntity latest) {
        criticalTemperatureService.handleCriticalTemp(deviceId, latest);
    }

    @Transactional
    public void handleHighTempDuration(Long deviceId, SensorDataEntity latest) {
        highTemperatureDurationService.evaluate(deviceId, latest);
    }

    public boolean isOffline(Long deviceId, OffsetDateTime lastSeenAt, OffsetDateTime now) {
        return offlineDetector.isOffline(deviceId, lastSeenAt, now);
    }

    @Transactional
    public void raiseOffline(Long deviceId) {
        alertLifecycleService.upsertActiveAlert(deviceId, null, AlertType.DEVICE_OFFLINE, "Device offline");
    }

    @Transactional
    public void resolveOffline(Long deviceId) {
        alertLifecycleService.resolveIfExists(deviceId, null, AlertType.DEVICE_OFFLINE);
    }
}
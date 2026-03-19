package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.AlertType;
import com.java.domain.SensorType;
import com.java.domain.service.dto.TelemetryPersistenceResult;
import com.java.persistence.entity.ConfigEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryAlertService {

    private final DeviceConfigService deviceConfigService;
    private final ThresholdStrategyFactory thresholdStrategyFactory;
    private final AlertService alertService;

    public void evaluateThresholds(TelemetryPersistenceResult result, Object rawValue) {
        if (result == null || result.device() == null || result.device().getHome() == null) {
            return;
        }

        ConfigEntity config = deviceConfigService.getActiveConfig(result.device().getId());
        var strategy = thresholdStrategyFactory.resolve(result.sensorType().name());
        if (strategy == null) {
            return;
        }

        strategy.evaluate(result.sensor(), config, rawValue).ifPresent(outcome -> {
            Long alertDeviceId = resolveAlertDeviceId(result, config);

            alertService.openOrRefresh(
                    result.device().getHome().getId(),
                    alertDeviceId,
                    result.sensor() != null ? result.sensor().getId() : null,
                    AlertType.valueOf(outcome.alertType()),
                    outcome.message()
            );
        });
    }

    private Long resolveAlertDeviceId(TelemetryPersistenceResult result, ConfigEntity config) {
        if (result == null || result.sensorType() == null || config == null) {
            return null;
        }

        SensorType sensorType = result.sensorType();

        if (sensorType == SensorType.TEMPERATURE || sensorType == SensorType.HUMIDITY) {
            return config.getMonitoringFanDevice() != null
                    ? config.getMonitoringFanDevice().getId()
                    : null;
        }

        if (sensorType == SensorType.LIGHT) {
            return config.getMonitoringLightDevice() != null
                    ? config.getMonitoringLightDevice().getId()
                    : null;
        }

        if (sensorType == SensorType.MOTION) {
            return null;
        }

        return null;
    }
}
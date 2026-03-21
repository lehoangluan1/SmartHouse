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
    private final AlertLifecycleService alertLifecycleService;
    private final HighTemperatureDurationService highTemperatureDurationService;

    public void evaluateThresholds(TelemetryPersistenceResult result, Object rawValue) {
        if (result == null || result.device() == null || result.device().getHome() == null) {
            return;
        }

        ConfigEntity config = deviceConfigService.getActiveConfig(result.device().getId());

        var strategy = thresholdStrategyFactory.resolve(result.sensorType().name());
        strategy.evaluate(result.sensor(), config, rawValue).ifPresent(outcome -> {
            Long alertDeviceId = resolveAlertDeviceId(result, config);

            if (alertDeviceId == null && result.sensor() == null) {
                return;
            }

            alertLifecycleService.upsertActiveAlert(
                    alertDeviceId,
                    result.sensor() != null ? result.sensor().getId() : null,
                    AlertType.valueOf(outcome.alertType()),
                    outcome.message()
            );
        });

        if (result.sensorType() == SensorType.TEMPERATURE && result.data() != null) {
            Long alertDeviceId = resolveAlertDeviceId(result, config);
            if (alertDeviceId != null) {
                highTemperatureDurationService.evaluate(alertDeviceId, result.data());
            }
        }
    }

    private Long resolveAlertDeviceId(TelemetryPersistenceResult result, ConfigEntity config) {
        if (result == null || result.sensorType() == null) {
            return null;
        }

        SensorType sensorType = result.sensorType();

        if (sensorType == SensorType.TEMPERATURE || sensorType == SensorType.HUMIDITY) {
            return config != null && config.getMonitoringFanDevice() != null
                    ? config.getMonitoringFanDevice().getId()
                    : result.device() != null ? result.device().getId() : null;
        }

        if (sensorType == SensorType.LIGHT) {
            return config != null && config.getMonitoringLightDevice() != null
                    ? config.getMonitoringLightDevice().getId()
                    : result.device() != null ? result.device().getId() : null;
        }

        if (sensorType == SensorType.MOTION) {
            return result.device() != null ? result.device().getId() : null;
        }

        return result.device() != null ? result.device().getId() : null;
    }
}
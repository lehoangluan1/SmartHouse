package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.SensorType;
import com.java.domain.service.dto.TelemetryPersistenceResult;
import com.java.persistence.entity.ConfigEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.repo.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryAutomationService {

    private final ModeAutomationService modeAutomationService;
    private final ConfigRepository configRepository;

    public void handle(TelemetryPersistenceResult result) {
        if (result == null || result.device() == null || result.device().getHome() == null) {
            return;
        }

        Long homeId = result.device().getHome().getId();
        ConfigEntity config = configRepository.findFirstByHomeIdAndIsActiveTrue(homeId)
                .or(() -> configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(homeId))
                .orElse(null);

        if (config == null) {
            log.info("AUTOMATION skip reason=no_config home={} sensorType={}", homeId, result.sensorType());
            return;
        }

        if (result.sensorType() == SensorType.TEMPERATURE) {
            triggerIfConfiguredSensor(
                    result,
                    config.getMonitoringTemperatureDevice(),
                    config.getMonitoringFanDevice(),
                    homeId
            );
        } else if (result.sensorType() == SensorType.LIGHT) {
            triggerIfConfiguredSensor(
                    result,
                    config.getMonitoringLightSensorDevice(),
                    config.getMonitoringLightDevice(),
                    homeId
            );
        }
    }

    private void triggerIfConfiguredSensor(
            TelemetryPersistenceResult result,
            DeviceEntity configuredSensor,
            DeviceEntity targetDevice,
            Long homeId
    ) {
        Long sourceDeviceId = result.device().getId();
        if (configuredSensor == null || configuredSensor.getId() == null
                || !configuredSensor.getId().equals(sourceDeviceId)) {
            log.info("AUTOMATION skip reason=unconfigured_sensor home={} sensorType={} sourceDevice={}",
                    homeId, result.sensorType(), result.device().getDeviceKey());
            return;
        }

        if (targetDevice == null || targetDevice.getId() == null) {
            log.info("AUTOMATION skip reason=no_target home={} sensorType={} sourceDevice={}",
                    homeId, result.sensorType(), result.device().getDeviceKey());
            return;
        }

        log.info("AUTOMATION evaluate home={} reason=telemetry sensorType={} sourceDevice={} targetDevice={}",
                homeId, result.sensorType(), result.device().getDeviceKey(), targetDevice.getDeviceKey());
        modeAutomationService.evaluateAndApply(targetDevice.getId(), "telemetry");
    }
}

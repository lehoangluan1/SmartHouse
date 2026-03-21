package com.java.domain.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.java.config.BadRequestException;
import com.java.controller.dto.TelemetryIngestRequest;
import com.java.domain.DeviceClass;
import com.java.domain.EntityStatus;
import com.java.domain.SensorType;
import com.java.domain.service.dto.TelemetryPersistenceResult;
import com.java.persistence.entity.SensorDataEntity;
import com.java.persistence.repo.DeviceRepository;
import com.java.persistence.repo.SensorDataRepository;
import com.java.persistence.repo.SensorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelemetryPersistenceService {

    private final DeviceRepository deviceRepository;
    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;
    private final TelemetryValueParser telemetryValueParser;
    private final TelemetryValidationService telemetryValidationService;
    private final DeviceRuntimeStateService deviceRuntimeStateService;

    @Transactional
    public TelemetryPersistenceResult persist(TelemetryIngestRequest request) {
        var device = deviceRepository.findByDeviceKey(request.deviceKey())
                .orElseThrow(() -> new BadRequestException("invalid deviceKey"));

        if (device.getDeviceClass() != DeviceClass.SENSOR_NODE) {
            throw new BadRequestException("Device is not a SENSOR_NODE: " + device.getDeviceKey());
        }

        var sensorType = telemetryValueParser.parseSensorType(request.sensorType());

        var sensor = sensorRepository.findByDeviceIdAndSensorKind(device.getId(), sensorType.name())
                .orElseThrow(() -> new BadRequestException("Device has no attached sensor: " + sensorType));

        var data = telemetryValueParser.buildSensorData(sensor, sensorType, request.value());

        telemetryValidationService.validateOrThrow(data);

        sensorDataRepository.save(data);

        Object runtimeValue = extractRuntimeValue(data);
        String capabilityCode = resolveCapabilityCode(sensorType);

        DeviceRuntimeStateService.StateWriteResult stateWriteResult
                = deviceRuntimeStateService.applyState(
                        device.getId(),
                        capabilityCode,
                        runtimeValue,
                        "telemetry",
                        data.getId(),
                        null
                );

        OffsetDateTime now = OffsetDateTime.now();
        device.setLastSeen(now);
        device.setIsOnline(Boolean.TRUE);
        if (device.getStatus() == null || device.getStatus() == EntityStatus.INACTIVE) {
            device.setStatus(EntityStatus.ACTIVE);
        }

        sensor.setLastSeen(now);
        if (sensor.getStatus() == null || sensor.getStatus() == EntityStatus.INACTIVE) {
            sensor.setStatus(EntityStatus.ACTIVE);
        }

        deviceRepository.save(device);
        sensorRepository.save(sensor);

        return new TelemetryPersistenceResult(
                device,
                sensor,
                data,
                sensorType,
                now,
                stateWriteResult
        );
    }

    private String resolveCapabilityCode(SensorType sensorType) {
        return switch (sensorType) {
            case TEMPERATURE -> "TEMPERATURE";
            case HUMIDITY -> "HUMIDITY";
            case LIGHT -> "BRIGHTNESS";
            case MOTION -> "MOTION";
            case DISTANCE -> "DISTANCE";
            case SMOKE -> "SMOKE";
            case OTHER -> throw new BadRequestException("No capability mapped for sensorType: " + sensorType);
        };
    }

    private Object extractRuntimeValue(SensorDataEntity data) {
        if (data.getValueBoolean() != null) return data.getValueBoolean();
        if (data.getValueNumeric() != null) return data.getValueNumeric();
        if (data.getValueText() != null) return data.getValueText();
        throw new BadRequestException("Telemetry payload does not contain a runtime value");
    }
}
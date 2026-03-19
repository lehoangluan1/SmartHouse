package com.java.domain.service.dto;

import java.time.OffsetDateTime;

import com.java.domain.SensorType;
import com.java.domain.service.DeviceRuntimeStateService;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.SensorDataEntity;
import com.java.persistence.entity.SensorEntity;

public record TelemetryPersistenceResult(
        DeviceEntity device,
        SensorEntity sensor,
        SensorDataEntity data,
        SensorType sensorType,
        OffsetDateTime persistedAt,
        DeviceRuntimeStateService.StateWriteResult stateWriteResult
) {
}
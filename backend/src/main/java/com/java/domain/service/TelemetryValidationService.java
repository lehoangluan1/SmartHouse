package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.domain.SensorType;
import com.java.persistence.entity.SensorDataEntity;


@Service
public class TelemetryValidationService {

    public boolean isValidTelemetry(SensorDataEntity data) {
        if (data == null || data.getSensor() == null || data.getSensor().getSensorKind() == null) {
            return false;
        }

        SensorType type = parseSensorType(data.getSensor().getSensorKind());

        return switch (type) {
            case TEMPERATURE -> data.getValueNumeric() != null
                    && data.getValueNumeric() >= -10
                    && data.getValueNumeric() <= 80;
            case HUMIDITY -> data.getValueNumeric() != null
                    && data.getValueNumeric() >= 0
                    && data.getValueNumeric() <= 100;
            case LIGHT -> data.getValueNumeric() != null
                    && data.getValueNumeric() >= 0;
            case MOTION -> data.getValueBoolean() != null;
            case DISTANCE, SMOKE, OTHER -> data.getValueNumeric() != null
                    || (data.getValueText() != null && !data.getValueText().isBlank())
                    || data.getValueBoolean() != null;
        };
    }

    private SensorType parseSensorType(String sensorKind) {
        try {
            return SensorType.valueOf(sensorKind.trim().toUpperCase());
        } catch (Exception e) {
            return SensorType.OTHER;
        }
    }
}
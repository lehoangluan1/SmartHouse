package com.java.domain.service;

import org.springframework.stereotype.Service;

import com.java.config.InvalidTelemetryException;
import com.java.domain.SensorType;
import com.java.persistence.entity.SensorDataEntity;

@Service
public class TelemetryValidationService {

    public void validateOrThrow(SensorDataEntity data) {
        if (data == null) {
            throw new InvalidTelemetryException("Telemetry data is null");
        }
        if (data.getSensor() == null || data.getSensor().getSensorKind() == null) {
            throw new InvalidTelemetryException("Telemetry sensor metadata is missing");
        }

        SensorType type = parseSensorType(data.getSensor().getSensorKind());

        switch (type) {
            case TEMPERATURE -> {
                Double v = data.getValueNumeric();
                if (v == null) {
                    throw new InvalidTelemetryException("Temperature value must be numeric");
                }
                if (v < -10 || v > 80) {
                    throw new InvalidTelemetryException("Temperature out of range: " + v + " (allowed -10..80)");
                }
            }
            case HUMIDITY -> {
                Double v = data.getValueNumeric();
                if (v == null) {
                    throw new InvalidTelemetryException("Humidity value must be numeric");
                }
                if (v < 0 || v > 100) {
                    throw new InvalidTelemetryException("Humidity out of range: " + v + " (allowed 0..100)");
                }
            }
            case LIGHT -> {
                Double v = data.getValueNumeric();
                if (v == null) {
                    throw new InvalidTelemetryException("Light value must be numeric");
                }
                if (v < 0 || v > 100000) {
                    throw new InvalidTelemetryException("Light out of range: " + v + " (allowed >= 0)");
                }
            }
            case MOTION -> {
                if (data.getValueBoolean() == null) {
                    throw new InvalidTelemetryException("Motion value must be boolean");
                }
            }
            case DISTANCE -> {
                Double v = data.getValueNumeric();
                if (v == null) {
                    throw new InvalidTelemetryException("Distance value must be numeric");
                }
                if (v < 0 || v > 1000) {
                    throw new InvalidTelemetryException("Distance out of range: " + v + " (allowed 0..1000)");
                }
            }
            case SMOKE, OTHER -> {
                if (data.getValueNumeric() == null
                        && data.getValueBoolean() == null
                        && (data.getValueText() == null || data.getValueText().isBlank())) {
                    throw new InvalidTelemetryException("Telemetry value is empty");
                }
            }
        }
    }

    private SensorType parseSensorType(String sensorKind) {
        try {
            return SensorType.valueOf(sensorKind.trim().toUpperCase());
        } catch (Exception e) {
            return SensorType.OTHER;
        }
    }
}
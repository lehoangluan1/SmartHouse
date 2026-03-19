package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.config.BadRequestException;
import com.java.domain.SensorType;
import com.java.persistence.entity.SensorDataEntity;
import com.java.persistence.entity.SensorEntity;

@Component
public class TelemetryValueParser {

    public SensorType parseSensorType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("sensorType must not be blank");
        }

        String normalized = raw.trim().toUpperCase();

        return switch (normalized) {
            case "TEMP", "TEMPERATURE" -> SensorType.TEMPERATURE;
            case "HUMID", "HUMIDITY" -> SensorType.HUMIDITY;
            case "LIGHT", "LUX" -> SensorType.LIGHT;
            case "PRESENCE", "MOTION", "PIR" -> SensorType.MOTION;
            case "DISTANCE" -> SensorType.DISTANCE;
            case "SMOKE", "GAS" -> SensorType.SMOKE;
            default -> throw new BadRequestException("invalid sensorType: " + raw);
        };
    }

    public SensorDataEntity buildSensorData(SensorEntity sensor, SensorType sensorType, Object value) {
        SensorDataEntity entity = new SensorDataEntity();
        entity.setSensor(sensor);
        assignValue(entity, sensorType, value);
        return entity;
    }

    private void assignValue(SensorDataEntity entity, SensorType sensorType, Object value) {
        entity.setValueNumeric(null);
        entity.setValueBoolean(null);
        entity.setValueText(null);

        if (sensorType == SensorType.MOTION) {
            entity.setValueBoolean(toBoolean(value));
            return;
        }

        if (value instanceof Number number) {
            entity.setValueNumeric(number.doubleValue());
            return;
        }

        if (value instanceof Boolean bool) {
            entity.setValueBoolean(bool);
            return;
        }

        String text = String.valueOf(value);

        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            entity.setValueBoolean(Boolean.valueOf(text));
            return;
        }

        try {
            entity.setValueNumeric(Double.valueOf(text));
        } catch (NumberFormatException ignored) {
            entity.setValueText(text);
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return false;

        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;

        String text = String.valueOf(value).trim().toLowerCase();
        return switch (text) {
            case "1", "true", "yes", "on", "motion", "detected" -> true;
            case "0", "false", "no", "off", "idle", "clear", "none" -> false;
            default -> false;
        };
    }
}
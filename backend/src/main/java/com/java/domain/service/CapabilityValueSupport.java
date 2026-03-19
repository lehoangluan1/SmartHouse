package com.java.domain.service;

import org.springframework.stereotype.Component;

import com.java.domain.CapabilityValueType;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceCapabilityEntity;
import com.java.persistence.entity.DeviceRuntimeStateEntity;

@Component
public class CapabilityValueSupport {

    public String runtimeValueAsString(DeviceRuntimeStateEntity state) {
        if (state == null) return null;
        if (state.getValueBoolean() != null) return String.valueOf(state.getValueBoolean());
        if (state.getValueNumber() != null) return stripTrailingZero(state.getValueNumber());
        return state.getValueText();
    }

    public String commandValueAsString(ControlCommandEntity entity) {
        if (entity == null) return null;
        if (entity.getValueBoolean() != null) return String.valueOf(entity.getValueBoolean());
        if (entity.getValueNumber() != null) return stripTrailingZero(entity.getValueNumber());
        return entity.getValueText();
    }

    public void assignRuntimeValue(DeviceRuntimeStateEntity entity, Object value) {
        entity.setValueBoolean(null);
        entity.setValueNumber(null);
        entity.setValueText(null);

        if (value == null) return;

        if (value instanceof Boolean b) {
            entity.setValueBoolean(b);
            return;
        }
        if (value instanceof Number n) {
            entity.setValueNumber(n.doubleValue());
            return;
        }
        entity.setValueText(String.valueOf(value));
    }

    public void assignCommandValue(ControlCommandEntity entity, Object value) {
        entity.setValueBoolean(null);
        entity.setValueNumber(null);
        entity.setValueText(null);

        if (value == null) return;

        if (value instanceof Boolean b) {
            entity.setValueBoolean(b);
            return;
        }
        if (value instanceof Number n) {
            entity.setValueNumber(n.doubleValue());
            return;
        }
        entity.setValueText(String.valueOf(value));
    }

    public Object parseValueByCapabilityType(DeviceCapabilityEntity capability, String raw) {
        if (capability == null || capability.getValueType() == null) {
            return raw;
        }

        CapabilityValueType type = capability.getValueType();
        return switch (type) {
            case BOOLEAN -> parseBoolean(raw);
            case NUMBER -> parseNumber(raw);
            case TEXT, MODE -> raw == null ? null : raw.trim();
        };
    }

    public Boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.trim().toLowerCase()) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + raw);
        };
    }

    public Double parseNumber(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Double.valueOf(raw.trim());
    }

    private String stripTrailingZero(Double value) {
        if (value == null) return null;
        if (value % 1 == 0) return String.valueOf(value.longValue());
        return String.valueOf(value);
    }
}
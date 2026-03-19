package com.java.domain.service;

import org.springframework.stereotype.Component;
import com.java.domain.AlertType;

@Component
public class AlertMessageFormatter {

    public String displayType(AlertType type) {
        if (type == null) {
            return "Alert";
        }

        return switch (type) {
            case CRITICAL_TEMP -> "Critical temperature";
            case DEVICE_OFFLINE -> "Device offline";
            case SENSOR_ERROR -> "Sensor error";
            case WRONG_PASSWORD -> "Incorrect password";
            case HIGH_HUMIDITY -> "High humidity";
            case LOW_HUMIDITY -> "Low humidity";
            case LOW_LIGHT -> "Low light";
            case HIGH_LIGHT -> "High light";
            case MOTION_DETECTED -> "Motion detected";
            case CUSTOM -> "Custom alert";
            case TEMP_TOO_HIGH -> "Temperature too high";
            case TEMP_TOO_LOW -> "Temperature too low";
        };
    }
}
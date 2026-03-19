package com.java.mapper;


import org.springframework.stereotype.Component;

@Component
public class DeviceRuntimeProjectionValueMapper {

    public String toOnOff(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "on" : "off";
        }

        String normalized = String.valueOf(value).trim().toLowerCase();
        if ("true".equals(normalized)) {
            return "on";
        }
        if ("false".equals(normalized)) {
            return "off";
        }
        if ("on".equals(normalized) || "off".equals(normalized)) {
            return normalized;
        }

        return null;
    }

    public Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String toText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
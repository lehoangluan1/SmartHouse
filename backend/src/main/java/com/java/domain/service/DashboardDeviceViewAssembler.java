package com.java.domain.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.java.controller.dto.DeviceResponse;
import com.java.persistence.entity.DeviceEntity;

@Component
public class DashboardDeviceViewAssembler {

    private static final String CAPABILITY_MODE = "MODE";
    private static final String CAPABILITY_POWER = "POWER";
    private static final String CAPABILITY_SPEED = "SPEED";
    private static final String CAPABILITY_BRIGHTNESS = "BRIGHTNESS";

    public DeviceResponse toResponse(DeviceEntity device, Map<String, Object> states) {
        String subtype = normalizeSubtype(device.getSubtype());

        DeviceResponse.DeviceResponseBuilder builder = DeviceResponse.builder()
                .id(device.getId())
                .name(device.getName())
                .deviceKey(device.getDeviceKey())
                .type(resolveType(device))
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .homeId(device.getHome() != null ? device.getHome().getId() : null)
                .roomName(device.getRoomName())
                .online(Boolean.TRUE.equals(device.getIsOnline()))
                .lastSeen(device.getLastSeen())
                .mode(asText(states.get(CAPABILITY_MODE)))
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt());

        switch (subtype) {
            case "FAN", "AIR_CONDITIONER" -> builder
                    .fanStatus(asOnOff(states.get(CAPABILITY_POWER)))
                    .fanSpeed(asInteger(states.get(CAPABILITY_SPEED)));

            case "LIGHT" -> builder
                    .lightStatus(asOnOff(states.get(CAPABILITY_POWER)))
                    .lightLevel(asInteger(states.get(CAPABILITY_BRIGHTNESS)));

            case "SMART_CONTROLLER", "CONTROLLER" -> {
                // chỉ mode
            }

            default -> {
                if (states.containsKey(CAPABILITY_SPEED)) {
                    builder.fanStatus(asOnOff(states.get(CAPABILITY_POWER)))
                           .fanSpeed(asInteger(states.get(CAPABILITY_SPEED)));
                }
                if (states.containsKey(CAPABILITY_BRIGHTNESS)) {
                    builder.lightStatus(asOnOff(states.get(CAPABILITY_POWER)))
                           .lightLevel(asInteger(states.get(CAPABILITY_BRIGHTNESS)));
                }
            }
        }

        return builder.build();
    }

    private String resolveType(DeviceEntity device) {
        if (device.getSubtype() != null && !device.getSubtype().isBlank()) {
            return device.getSubtype();
        }
        return device.getDeviceClass() != null ? device.getDeviceClass().name() : null;
    }

    private String normalizeSubtype(String subtype) {
        return subtype == null ? "" : subtype.trim().toUpperCase();
    }

    private String asText(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private String asOnOff(Object value) {
        if (!(value instanceof Boolean boolValue)) {
            return null;
        }
        return boolValue ? "on" : "off";
    }

    private Integer asInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return number.intValue();
    }
}
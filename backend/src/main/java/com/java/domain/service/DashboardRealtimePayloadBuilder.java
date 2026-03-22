package com.java.domain.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DashboardRealtimePayloadBuilder {

    public Map<String, Object> connected(Long homeId) {
        Map<String, Object> payload = basePayload("CONNECTED", homeId, null);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("connectedAt", OffsetDateTime.now().toString());

        payload.put("payload", innerPayload);
        return payload;
    }

    public Map<String, Object> telemetryReceived(
            Long homeId,
            Long deviceId,
            Double valueNumeric,
            String valueText,
            Boolean valueBoolean,
            OffsetDateTime createdAt
    ) {
        Map<String, Object> payload = basePayload("TELEMETRY_RECEIVED", homeId, deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("valueNumeric", valueNumeric);
        innerPayload.put("valueText", valueText);
        innerPayload.put("valueBoolean", valueBoolean);
        innerPayload.put(
                "createdAt",
                createdAt == null ? OffsetDateTime.now().toString() : createdAt.toString()
        );

        payload.put("payload", innerPayload);
        return payload;
    }

    public Map<String, Object> deviceStateChanged(
            Long homeId,
            Long deviceId,
            String status,
            Integer speed,
            Integer brightness
    ) {
        Map<String, Object> payload = basePayload("DEVICE_STATE_CHANGED", homeId, deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        if (status != null) {
            innerPayload.put("status", status);
        }
        if (speed != null) {
            innerPayload.put("speed", speed);
        }
        if (brightness != null) {
            innerPayload.put("brightness", brightness);
        }

        payload.put("payload", innerPayload);
        return payload;
    }

    public Map<String, Object> homeModeChanged(Long homeId, Long deviceId, String mode) {
        Map<String, Object> payload = basePayload("HOME_MODE_CHANGED", homeId, deviceId);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("mode", mode == null ? null : mode.trim().toUpperCase());

        payload.put("payload", innerPayload);
        return payload;
    }

    public Map<String, Object> heartbeat(Long homeId) {
        Map<String, Object> payload = basePayload("HEARTBEAT", homeId, null);

        Map<String, Object> innerPayload = new LinkedHashMap<>();
        innerPayload.put("ts", OffsetDateTime.now().toString());

        payload.put("payload", innerPayload);
        return payload;
    }

    private Map<String, Object> basePayload(String type, Long homeId, Long deviceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("homeId", homeId);

        if (deviceId != null) {
            payload.put("deviceId", deviceId);
        }

        return payload;
    }
}